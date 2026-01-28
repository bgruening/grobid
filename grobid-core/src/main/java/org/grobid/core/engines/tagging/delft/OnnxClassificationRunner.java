package org.grobid.core.engines.tagging.delft;

import ai.onnxruntime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * ONNX Runtime wrapper for running DeLFT classification models.
 * 
 * Handles low-level ONNX operations: loading models, creating tensors,
 * running inference, and managing resources. The higher-level
 * OnnxClassificationModel handles embeddings, tokenization, and output
 * formatting.
 */
public class OnnxClassificationRunner implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnnxClassificationRunner.class);

    private final OrtEnvironment env;
    private final OrtSession session;
    private final String inputName;

    /**
     * Load an ONNX classification model.
     * 
     * @param modelPath Path to the .onnx file
     */
    public OnnxClassificationRunner(Path modelPath) throws OrtException {
        this.env = OrtEnvironment.getEnvironment();

        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);

        // Configure threading for optimal CPU inference performance
        // Since GROBID manages concurrency at the worker level (e.g., 10 concurrent
        // workers), use single-threaded inference per session to avoid CPU
        // oversubscription
        options.setIntraOpNumThreads(1);

        // interOpNumThreads: threads for parallel execution of multiple operators
        // Set to 1 since GROBID manages concurrency at a higher level
        options.setInterOpNumThreads(1);

        // Use sequential execution mode (vs parallel) since GROBID handles parallelism
        options.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL);

        this.session = env.createSession(modelPath.toString(), options);

        // Get the first input name (classification models typically have one input)
        this.inputName = session.getInputNames().iterator().next();

        LOGGER.info("Loaded ONNX classification model from {} (single-threaded, sequential mode)", modelPath);
        LOGGER.info("Input names: {}", session.getInputNames());
        LOGGER.info("Output names: {}", session.getOutputNames());
    }

    /**
     * Run inference on embeddings input.
     * 
     * @param embeddings Input embeddings [batch, maxlen, embeddingSize]
     * @return Raw logits [batch, numClasses] (not yet softmax-normalized)
     */
    public float[][] runInference(float[][][] embeddings) throws OrtException {
        int batchSize = embeddings.length;
        int maxlen = embeddings[0].length;
        int embeddingSize = embeddings[0][0].length;

        // Flatten embeddings for ONNX tensor
        float[] flat = flatten3D(embeddings);

        // Create input tensor
        OnnxTensor inputTensor = OnnxTensor.createTensor(env,
                FloatBuffer.wrap(flat),
                new long[] { batchSize, maxlen, embeddingSize });

        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put(inputName, inputTensor);

        try (OrtSession.Result result = session.run(inputs)) {
            // Get output - should be [batch, numClasses] (raw logits)
            OnnxTensor outputTensor = (OnnxTensor) result.get(0);
            long[] shape = outputTensor.getInfo().getShape();
            int numClasses = (int) shape[1];

            float[] outputFlat = outputTensor.getFloatBuffer().array();

            // Reshape to 2D
            float[][] logits = new float[batchSize][numClasses];
            int idx = 0;
            for (int b = 0; b < batchSize; b++) {
                for (int c = 0; c < numClasses; c++) {
                    logits[b][c] = outputFlat[idx++];
                }
            }

            return logits;
        } finally {
            inputTensor.close();
        }
    }

    /**
     * Flatten 3D array to 1D for ONNX tensor creation.
     */
    private float[] flatten3D(float[][][] arr) {
        int d1 = arr.length;
        int d2 = arr[0].length;
        int d3 = arr[0][0].length;
        float[] flat = new float[d1 * d2 * d3];
        int idx = 0;
        for (int i = 0; i < d1; i++) {
            for (int j = 0; j < d2; j++) {
                for (int k = 0; k < d3; k++) {
                    flat[idx++] = arr[i][j][k];
                }
            }
        }
        return flat;
    }

    @Override
    public void close() {
        try {
            if (session != null) {
                session.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Error closing ONNX session", e);
        }
    }
}

package org.grobid.trainer;

import java.io.File;

import org.grobid.core.GrobidModel;

public interface GenericTrainer {
    void train(File template, File trainingData, File outputModel, int numThreads, GrobidModel model);

    void train(
            File template,
            File trainingData,
            File outputModel,
            int numThreads,
            GrobidModel model,
            boolean incremental);

    String getName();

    public void setEpsilon(double epsilon);

    public void setWindow(int window);

    public double getEpsilon();

    public int getWindow();

    public int getNbMaxIterations();

    public void setNbMaxIterations(int iterations);

    public double getL1();

    public void setL1(double l1);

    public double getL2();

    public void setL2(double l2);
}

# Typed Areas API Documentation

The Typed Areas API provides enhanced control over PDF document processing by allowing you to specify regions for specialized handling. This feature improves accuracy for figure and table extraction while providing control over content exclusion.

## Overview

Instead of relying solely on automatic detection, you can pre-identify areas containing:
- **Figures** - processed with specialized figure model
- **Tables** - processed with specialized table model
- **Ignored content** - completely excluded from processing

## Supported Endpoints

The `typedAreas` parameter is supported by the following endpoints:

- `POST /api/processHeaderDocument` - Header extraction with typed areas
- `POST /api/processFulltextDocument` - Full document processing with typed areas
- `POST /api/processReferences` - Reference extraction with typed areas

## Request Format

### Parameter Specification

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `typedAreas` | string (JSON) | No | JSON array of area specifications |

### JSON Structure

```json
[
  {
    "page": 1,
    "x": 100.0,
    "y": 200.0,
    "width": 300.0,
    "height": 150.0,
    "type": "figure"
  },
  {
    "page": 1,
    "x": 450.0,
    "y": 200.0,
    "width": 250.0,
    "height": 200.0,
    "type": "table"
  },
  {
    "page": 1,
    "x": 50.0,
    "y": 500.0,
    "width": 500.0,
    "height": 100.0,
    "type": "ignore"
  }
]
```

### Field Descriptions

- **`page`** (integer, required): Page number (1-based, following PDF convention)
- **`x`** (number, required): X-coordinate of upper-left corner in points
- **`y`** (number, required): Y-coordinate of upper-left corner in points
- **`width`** (number, required): Width of the area in points
- **`height`** (number, required): Height of the area in points
- **`type`** (string, required): Area type - `"figure"`, `"table"`, or `"ignore"`

## Area Types and Processing

### Figure Areas (`"type": "figure"`)

**Processing**:
- Tokens within figure areas are extracted from main text processing
- Applied to specialized FigureParser model
- Results integrated into TEI output as structured `<figure>` elements
- Bypasses segmentation model for improved accuracy

**Use Cases**:
- Pre-identified figures from external OCR or layout analysis
- Complex diagrams where automatic detection fails
- Figures with known boundaries for consistent processing

### Table Areas (`"type": "table"`)

**Processing**:
- Tokens within table areas are extracted from main text processing
- Applied to specialized TableParser model
- Results integrated into TEI output as structured `<table>` elements
- Bypasses segmentation model for improved accuracy

**Use Cases**:
- Tables with complex layouts or formatting
- Pre-identified table regions from document analysis
- Tables requiring consistent extraction across documents

### Ignore Areas (`"type": "ignore"`)

**Processing**:
- Tokens within ignore areas are completely discarded
- No further processing performed on these regions
- Content excluded from all model processing

**Use Cases**:
- Headers, footers, and page numbers
- Watermarks or background elements
- Marginalia or annotations
- Advertisements or irrelevant content

## Coordinate System

The coordinate system follows PDF conventions:

```
(0,0) +----------------------→ X (points)
      |
      |
      ↓ Y (points)
```

- **Origin**: Upper-left corner of the page
- **Units**: Points (1/72 inch ≈ 0.353 mm)
- **Page numbering**: 1-based (first page is page 1)

## Usage Examples

### cURL Examples

**Basic header processing with typed areas:**
```bash
curl -v -H "Accept: application/xml" \
  --form input=@./document.pdf \
  --form typedAreas='[
    {"page": 1, "x": 100, "y": 200, "width": 300, "height": 150, "type": "figure"},
    {"page": 1, "x": 450, "y": 200, "width": 250, "height": 200, "type": "table"}
  ]' \
  localhost:8070/api/processHeaderDocument
```

**Full document processing with areas:**
```bash
curl -v -H "Accept: application/xml" \
  --form input=@./document.pdf \
  --form consolidateHeader=1 \
  --form typedAreas='[
    {"page": 1, "x": 50, "y": 750, "width": 500, "height": 50, "type": "ignore"},
    {"page": 2, "x": 100, "y": 100, "width": 400, "height": 300, "type": "figure"}
  ]' \
  localhost:8070/api/processFulltextDocument
```

### Python Examples

**Using requests library:**
```python
import requests
import json

# Define typed areas
typed_areas = [
    {"page": 1, "x": 100, "y": 200, "width": 300, "height": 150, "type": "figure"},
    {"page": 1, "x": 450, "y": 200, "width": 250, "height": 200, "type": "table"},
    {"page": 1, "x": 50, "y": 750, "width": 500, "height": 30, "type": "ignore"}
]

# Process document
with open('document.pdf', 'rb') as f:
    files = {'input': f}
    data = {
        'typedAreas': json.dumps(typed_areas),
        'consolidateHeader': '1',
        'segmentSentences': '1'
    }

    response = requests.post(
        'http://localhost:8070/api/processFulltextDocument',
        files=files,
        data=data,
        headers={'Accept': 'application/xml'}
    )

    if response.status_code == 200:
        print(response.text)
    else:
        print(f"Error: {response.status_code} - {response.text}")
```

**Complex processing with multiple parameters:**
```python
import requests
import json

def process_with_typed_areas(pdf_path, typed_areas, endpoint="processFulltextDocument"):
    """Process a PDF with typed areas and additional parameters."""

    url = f"http://localhost:8070/api/{endpoint}"

    with open(pdf_path, 'rb') as f:
        files = {'input': f}
        data = {
            'typedAreas': json.dumps(typed_areas),
            'consolidateHeader': '1',
            'consolidateCitations': '1',
            'segmentSentences': '1',
            'generateIDs': '1',
            'includeRawCitations': '1'
        }

        response = requests.post(
            url,
            files=files,
            data=data,
            headers={'Accept': 'application/xml'}
        )

        return response

# Example usage
figure_areas = [
    {"page": 1, "x": 85, "y": 120, "width": 440, "height": 280, "type": "figure"},
    {"page": 2, "x": 85, "y": 200, "width": 300, "height": 200, "type": "table"}
]

response = process_with_typed_areas("research_paper.pdf", figure_areas)
print(response.status_code)
```

### JavaScript Examples

**Using fetch API:**
```javascript
async function processWithTypedAreas(pdfFile, typedAreas) {
    const formData = new FormData();
    formData.append('input', pdfFile);
    formData.append('typedAreas', JSON.stringify(typedAreas));
    formData.append('consolidateHeader', '1');
    formData.append('segmentSentences', '1');

    try {
        const response = await fetch(
            'http://localhost:8070/api/processFulltextDocument',
            {
                method: 'POST',
                body: formData,
                headers: {
                    'Accept': 'application/xml'
                }
            }
        );

        if (response.ok) {
            const result = await response.text();
            return result;
        } else {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
    } catch (error) {
        console.error('Error processing document:', error);
        throw error;
    }
}

// Usage example
const typedAreas = [
    {page: 1, x: 100, y: 200, width: 300, height: 150, type: "figure"},
    {page: 1, x: 450, y: 200, width: 250, height: 200, type: "table"}
];

const fileInput = document.getElementById('pdf-input');
fileInput.addEventListener('change', async (event) => {
    const file = event.target.files[0];
    if (file) {
        try {
            const result = await processWithTypedAreas(file, typedAreas);
            console.log('Processing result:', result);
        } catch (error) {
            console.error('Processing failed:', error);
        }
    }
});
```

## Error Handling

### Common Error Scenarios

**Invalid JSON format:**
```json
// Invalid - missing quotes around type
{"page": 1, "x": 100, "y": 200, "width": 300, "height": 150, type: figure}
```
**Error**: HTTP 400 - "Invalid JSON format"

**Invalid area type:**
```json
// Invalid - unsupported area type
{"page": 1, "x": 100, "y": 200, "width": 300, "height": 150, "type": "diagram"}
```
**Behavior**: Area logged as warning and skipped

**Missing required fields:**
```json
// Invalid - missing type field
{"page": 1, "x": 100, "y": 200, "width": 300, "height": 150}
```
**Behavior**: Area logged as warning and skipped

**Invalid coordinates:**
```json
// Valid but outside bounds - will be clamped
{"page": 1, "x": -100, "y": 200, "width": 300, "height": 150, "type": "figure"}
```
**Behavior**: Coordinates clamped to valid page boundaries

### Response Status Codes

| Status Code | Description |
|-------------|-------------|
| 200 | Successful processing with typed areas |
| 204 | Processing completed but no content extracted |
| 400 | Invalid request (malformed JSON, missing parameters) |
| 500 | Internal server error during processing |
| 503 | Service unavailable (all threads in use) |

## Integration with Existing Workflow

### Combining with Other Parameters

Typed areas work seamlessly with all existing GROBID parameters:

```bash
curl -v -H "Accept: application/xml" \
  --form input=@./document.pdf \
  --form typedAreas='[{"page": 1, "x": 100, "y": 200, "width": 300, "height": 150, "type": "figure"}]' \
  --form consolidateHeader=1 \
  --form consolidateCitations=1 \
  --form segmentSentences=1 \
  --form generateIDs=1 \
  --form teiCoordinates=figure,table \
  localhost:8070/api/processFulltextDocument
```

### TEI Output Structure

Processed typed areas are integrated into the standard TEI output:

```xml
<figure>
    <head>Figure 1: Sample Figure</head>
    <figDesc>Description extracted from specialized processing</figDesc>
    <graphic url="#img1"/>
</figure>

<table>
    <head>Table 1: Sample Data</head>
    <row>
        <cell>Header 1</cell>
        <cell>Header 2</cell>
    </row>
    <!-- Table content from specialized processing -->
</table>
```

## Performance Considerations

### Optimization Tips

1. **Area Size**: Define areas as tightly as possible around content
2. **Overlapping Areas**: Avoid overlapping typed areas - results may be unpredictable
3. **Large Documents**: Consider processing pages individually for very large documents
4. **Batch Processing**: Reuse area definitions across similar documents when possible

### Performance Impact

- **Improved**: Bypassing segmentation for pre-identified areas
- **Overhead**: JSON parsing and area coordinate calculations
- **Memory**: Additional token lists for different area types
- **Overall**: Typically faster processing for documents with many pre-identified figures/tables

## Migration from Legacy ignoreAreas

The new `typedAreas` parameter replaces the legacy `ignoreAreas` parameter:

**Old format (deprecated):**
```json
[
  {"page": 1, "x": 100, "y": 200, "width": 300, "height": 150, "name": "figure"}
]
```

**New format (required):**
```json
[
  {"page": 1, "x": 100, "y": 200, "width": 300, "height": 150, "type": "figure"},
  {"page": 1, "x": 400, "y": 200, "width": 200, "height": 100, "type": "ignore"}
]
```

**Key Changes:**
- `name` field replaced with required `type` field
- Support for figure and table processing (not just ignoring)
- Type-safe area classification with enum validation

## Troubleshooting

### Common Issues

1. **Areas not being processed**: Check JSON format and field names
2. **Incorrect coordinates**: Verify coordinate system and page numbering
3. **Partial extraction**: Ensure areas fully encompass target content
4. **Performance issues**: Reduce number of areas or make them more precise

### Debugging Tips

1. **Start simple**: Test with a single, well-defined area
2. **Verify coordinates**: Use PDF viewer to confirm area boundaries
3. **Check logs**: Server logs provide detailed error messages for invalid areas
4. **Validate JSON**: Use JSON validator to ensure correct syntax

### Getting Help

- **Documentation**: See [GROBID Service API](Grobid-service.md) for general API usage
- **Issues**: Report bugs or request features via GitHub issues
- **Community**: Join discussions for usage tips and best practices
# Benchmarking biorXiv

## General

This is the end-to-end benchmarking result for GROBID version **0.8.2** against the `bioRxiv` test set (
`biorxiv-10k-test-2000`), see the [End-to-end evaluation](End-to-end-evaluation.md) page for explanations and for
reproducing this evaluation.

The following end-to-end results are using:

- **BidLSTM_ChainCRF_FEATURES** as sequence labeling for the header model

- **BidLSTM_ChainCRF_FEATURES** as sequence labeling for the reference-segmenter model

- **BidLSTM-CRF-FEATURES** as sequence labeling for the citation model

- **BidLSTM_CRF_FEATURES** as sequence labeling for the affiliation-address model

- **CRF Wapiti** as sequence labelling engine for all other models.

Header extractions are consolidated by default with [biblio-glutton](https://github.com/kermitt2/biblio-glutton)
service (the results with CrossRef REST API as consolidation service should be similar but much slower).

Other versions of these benchmarks with variants and **Deep Learning models** (e.g. newer master snapshots) are
available [here](https://github.com/kermitt2/grobid/tree/master/grobid-trainer/doc). Note that Deep Learning models
might provide higher accuracy, but at the cost of slower runtime and more expensive CPU/GPU resources.

Evaluation on 2000 PDF preprints out of 2000 (no failure).

Runtime for processing 2000 PDF: **1713** seconds (0.85 seconds per PDF file) on Ubuntu 22.04, 16 CPU (32 threads),
128GB RAM and with a GeForce GTX 1080 Ti GPU.

Note: with CRF only models runtime is 622s (0.31 second per PDF) with 4 CPU, 8 threads.

## Header metadata

Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 1.95      | 1.91      | 1.93      | 1990    |
| authors                     | 84.15     | 82.89     | 83.52     | 1999    |
| first_author                | 96.09     | 94.74     | 95.41     | 1997    |
| keywords                    | 47.37     | 45.05     | 46.18     | 839     |
| title                       | 75.92     | 74.4      | 75.15     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **63.11** | **61.79** | **62.44** | 8825    |
| all fields (macro avg.)     | 61.1      | 59.8      | 60.44     | 8825    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 57.48     | 56.18     | 56.82     | 1990    |
| authors                     | 84.61     | 83.34     | 83.97     | 1999    |
| first_author                | 96.19     | 94.84     | 95.51     | 1997    |
| keywords                    | 52.26     | 49.7      | 50.95     | 839     |
| title                       | 78.42     | 76.85     | 77.63     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **76.75** | **75.15** | **75.94** | 8825    |
| all fields (macro avg.)     | 73.79     | 72.18     | 72.98     | 8825    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 85.45     | 83.52     | 84.47     | 1990    |
| authors                     | 91.92     | 90.55     | 91.23     | 1999    |
| first_author                | 96.5      | 95.14     | 95.81     | 1997    |
| keywords                    | 77.94     | 74.14     | 75.99     | 839     |
| title                       | 91.84     | 90        | 90.91     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **90.2**  | **88.32** | **89.25** | 8825    |
| all fields (macro avg.)     | 88.73     | 86.67     | 87.68     | 8825    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 81.8      | 79.95     | 80.86     | 1990    |
| authors                     | 87.76     | 86.44     | 87.1      | 1999    |
| first_author                | 96.09     | 94.74     | 95.41     | 1997    |
| keywords                    | 63.03     | 59.95     | 61.45     | 839     |
| title                       | 86.94     | 85.2      | 86.06     | 2000    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **85.85** | **84.06** | **84.94** | 8825    |
| all fields (macro avg.)     | 83.12     | 81.26     | 82.18     | 8825    |

#### Instance-level results

```
Total expected instances: 	2000
Total correct instances: 	32 (strict) 
Total correct instances: 	624 (soft) 
Total correct instances: 	1255 (Levenshtein) 
Total correct instances: 	1034 (ObservedRatcliffObershelp) 

Instance-level recall:	1.6	(strict) 
Instance-level recall:	31.2	(soft) 
Instance-level recall:	62.75	(Levenshtein) 
Instance-level recall:	51.7	(RatcliffObershelp) 
```

## Citation metadata

Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 87.26     | 81.11     | 84.07     | 97183   |
| date                        | 90.78     | 84.01     | 87.26     | 97630   |
| doi                         | 68.31     | 75.43     | 71.69     | 16894   |
| first_author                | 94.26     | 87.55     | 90.78     | 97183   |
| inTitle                     | 82.48     | 78.11     | 80.23     | 96430   |
| issue                       | 92.34     | 84.87     | 88.45     | 30312   |
| page                        | 94.38     | 77.78     | 85.28     | 88597   |
| pmcid                       | 66.24     | 82.65     | 73.54     | 807     |
| pmid                        | 68.23     | 81.56     | 74.3      | 2093    |
| title                       | 84.61     | 82.04     | 83.3      | 92463   |
| volume                      | 95.36     | 93.56     | 94.45     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.09** | **83.26** | **86.08** | 707301  |
| all fields (macro avg.)     | 84.02     | 82.61     | 83.03     | 707301  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 88.39     | 82.17     | 85.17     | 97183   |
| date                        | 90.78     | 84.01     | 87.26     | 97630   |
| doi                         | 72.83     | 80.41     | 76.43     | 16894   |
| first_author                | 94.67     | 87.93     | 91.17     | 97183   |
| inTitle                     | 91.9      | 87.03     | 89.4      | 96430   |
| issue                       | 92.34     | 84.87     | 88.45     | 30312   |
| page                        | 94.38     | 77.78     | 85.28     | 88597   |
| pmcid                       | 75.17     | 93.8      | 83.46     | 807     |
| pmid                        | 72.7      | 86.91     | 79.17     | 2093    |
| title                       | 92.89     | 90.07     | 91.46     | 92463   |
| volume                      | 95.36     | 93.56     | 94.45     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **91.89** | **85.87** | **88.78** | 707301  |
| all fields (macro avg.)     | 87.4      | 86.23     | 86.52     | 707301  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 93.71     | 87.12     | 90.3      | 97183   |
| date                        | 90.78     | 84.01     | 87.26     | 97630   |
| doi                         | 76.99     | 85.01     | 80.8      | 16894   |
| first_author                | 94.82     | 88.06     | 91.32     | 97183   |
| inTitle                     | 92.84     | 87.92     | 90.31     | 96430   |
| issue                       | 92.34     | 84.87     | 88.45     | 30312   |
| page                        | 94.38     | 77.78     | 85.28     | 88597   |
| pmcid                       | 75.17     | 93.8      | 83.46     | 807     |
| pmid                        | 72.74     | 86.96     | 79.22     | 2093    |
| title                       | 95.79     | 92.88     | 94.31     | 92463   |
| volume                      | 95.36     | 93.56     | 94.45     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.27** | **87.17** | **90.12** | 707301  |
| all fields (macro avg.)     | 88.63     | 87.45     | 87.74     | 707301  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 90.6      | 84.22     | 87.29     | 97183   |
| date                        | 90.78     | 84.01     | 87.26     | 97630   |
| doi                         | 74.72     | 82.51     | 78.42     | 16894   |
| first_author                | 94.31     | 87.59     | 90.82     | 97183   |
| inTitle                     | 90.71     | 85.9      | 88.24     | 96430   |
| issue                       | 92.34     | 84.87     | 88.45     | 30312   |
| page                        | 94.38     | 77.78     | 85.28     | 88597   |
| pmcid                       | 66.24     | 82.65     | 73.54     | 807     |
| pmid                        | 68.23     | 81.56     | 74.3      | 2093    |
| title                       | 95.12     | 92.24     | 93.66     | 92463   |
| volume                      | 95.36     | 93.56     | 94.45     | 87709   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.3**  | **86.26** | **89.18** | 707301  |
| all fields (macro avg.)     | 86.62     | 85.17     | 85.61     | 707301  |

#### Instance-level results

```
Total expected instances: 		98799
Total extracted instances: 		98373
Total correct instances: 		41164 (strict) 
Total correct instances: 		51569 (soft) 
Total correct instances: 		55890 (Levenshtein) 
Total correct instances: 		52763 (RatcliffObershelp) 

Instance-level precision:	41.84 (strict) 
Instance-level precision:	52.42 (soft) 
Instance-level precision:	56.81 (Levenshtein) 
Instance-level precision:	53.64 (RatcliffObershelp) 

Instance-level recall:	41.66	(strict) 
Instance-level recall:	52.2	(soft) 
Instance-level recall:	56.57	(Levenshtein) 
Instance-level recall:	53.4	(RatcliffObershelp) 

Instance-level f-score:	41.75 (strict) 
Instance-level f-score:	52.31 (soft) 
Instance-level f-score:	56.69 (Levenshtein) 
Instance-level f-score:	53.52 (RatcliffObershelp) 

Matching 1 :	77036

Matching 2 :	4317

Matching 3 :	4784

Matching 4 :	2654

Total matches :	88791
```

#### Citation context resolution

```

Total expected references: 	 98797 - 49.4 references per article
Total predicted references: 	 98373 - 49.19 references per article

Total expected citation contexts: 	 142862 - 71.43 citation contexts per article
Total predicted citation contexts: 	 133085 - 66.54 citation contexts per article

Total correct predicted citation contexts: 	 113412 - 56.71 citation contexts per article
Total wrong predicted citation contexts: 	 19673 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 85.22
Recall citation contexts: 	 79.39
fscore citation contexts: 	 82.2
```

## Fulltext structures

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from
the actual PDF content and are can be inconsistent from one document to another. The scores of the following metrics are
thus not very meaningful in absolute term, in particular for the strict matching (textual content of the srtructure can
be very long). As relative values for comparing different models, they seem however useful.

Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 31.25     | 25.78     | 28.26     | 446     |
| figure_title                | 4.3       | 2.35      | 3.04      | 22978   |
| funding_stmt                | 3.6       | 23.29     | 6.23      | 747     |
| reference_citation          | 71.93     | 70.71     | 71.32     | 147470  |
| reference_figure            | 70.39     | 77.15     | 73.61     | 47984   |
| reference_table             | 45.64     | 86.79     | 59.82     | 5957    |
| section_title               | 71.29     | 69.91     | 70.59     | 32398   |
| table_title                 | 7.41      | 2.7       | 3.96      | 3925    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **65.44** | **64.93** | **65.18** | 261905  |
| all fields (macro avg.)     | 38.23     | 44.84     | 39.6      | 261905  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall   | f1       | support |
|-----------------------------|-----------|----------|----------|---------|
| availability_stmt           | 53.26     | 43.95    | 48.16    | 446     |
| figure_title                | 68.25     | 37.29    | 48.23    | 22978   |
| funding_stmt                | 3.83      | 24.77    | 6.63     | 747     |
| reference_citation          | 84.29     | 82.86    | 83.57    | 147470  |
| reference_figure            | 71.02     | 77.85    | 74.28    | 47984   |
| reference_table             | 46.05     | 87.58    | 60.36    | 5957    |
| section_title               | 76.85     | 75.36    | 76.1     | 32398   |
| table_title                 | 82.73     | 30.14    | 44.18    | 3925    |
|                             |           |          |          |         |
| **all fields (micro avg.)** | **76.7**  | **76.1** | **76.4** | 261905  |
| all fields (macro avg.)     | 60.78     | 57.47    | 55.19    | 261905  |

**Document-level ratio results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 86.18     | 82.51     | 84.31     | 446     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **86.18** | **82.51** | **84.31** | 446     |
| all fields (macro avg.)     | 86.18     | 82.51     | 84.31     | 446     |

Evaluation metrics produced in 1605.909 seconds


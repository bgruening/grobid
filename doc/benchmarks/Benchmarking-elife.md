# Benchmarking eLife

## General

This is the end-to-end benchmarking result for GROBID version **0.8.2** against the `eLife` test set, see
the [End-to-end evaluation](End-to-end-evaluation.md) page for explanations and for reproducing this evaluation.

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

Evaluation on 984 PDF preprints out of 984 (no failure).

Runtime for processing 984 PDF: **1131** seconds (1.15 seconds per PDF file) on Ubuntu 22.04, 16 CPU (32 threads), 128GB
RAM and with a GeForce GTX 1080 Ti GPU.

Note: with CRF only models runtime is 492s (0.50 seconds per PDF) with 4 CPU, 8 threads.

## Header metadata

Evaluation on 984 random PDF files out of 982 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 8.32      | 7.93      | 8.12      | 984     |
| authors                     | 41.67     | 40.69     | 41.17     | 983     |
| first_author                | 91.15     | 89.1      | 90.11     | 982     |
| title                       | 76.89     | 73.37     | 75.09     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **54.66** | **52.76** | **53.69** | 3933    |
| all fields (macro avg.)     | 54.51     | 52.77     | 53.62     | 3933    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 21.13     | 20.12     | 20.61     | 984     |
| authors                     | 41.77     | 40.79     | 41.28     | 983     |
| first_author                | 91.15     | 89.1      | 90.11     | 982     |
| title                       | 86.05     | 82.11     | 84.04     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **60.12** | **58.02** | **59.05** | 3933    |
| all fields (macro avg.)     | 60.02     | 58.03     | 59.01     | 3933    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| abstract                    | 46.53     | 44.31     | 45.39    | 984     |
| authors                     | 71.77     | 70.09     | 70.92    | 983     |
| first_author                | 91.88     | 89.82     | 90.83    | 982     |
| title                       | 95.74     | 91.36     | 93.5     | 984     |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **76.55** | **73.89** | **75.2** | 3933    |
| all fields (macro avg.)     | 76.48     | 73.89     | 75.16    | 3933    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 43.76     | 41.67     | 42.69     | 984     |
| authors                     | 51.56     | 50.36     | 50.95     | 983     |
| first_author                | 91.15     | 89.1      | 90.11     | 982     |
| title                       | 93.29     | 89.02     | 91.11     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **69.97** | **67.53** | **68.73** | 3933    |
| all fields (macro avg.)     | 69.94     | 67.54     | 68.71     | 3933    |

#### Instance-level results

```
Total expected instances: 	984
Total correct instances: 	11 (strict) 
Total correct instances: 	38 (soft) 
Total correct instances: 	257 (Levenshtein) 
Total correct instances: 	156 (ObservedRatcliffObershelp) 

Instance-level recall:	1.12	(strict) 
Instance-level recall:	3.86	(soft) 
Instance-level recall:	26.12	(Levenshtein) 
Instance-level recall:	15.85	(RatcliffObershelp) 
```

## Citation metadata

Evaluation on 984 random PDF files out of 982 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 77.14     | 75.27     | 76.2      | 63265   |
| date                        | 94.9      | 91.62     | 93.23     | 63662   |
| first_author                | 92.48     | 90.1      | 91.27     | 63265   |
| inTitle                     | 94.56     | 92.81     | 93.68     | 63213   |
| issue                       | 1.58      | 81.25     | 3.09      | 16      |
| page                        | 95.14     | 93.32     | 94.23     | 53375   |
| title                       | 89.52     | 88.7      | 89.11     | 62044   |
| volume                      | 97.08     | 96.22     | 96.64     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **91.28** | **89.61** | **90.43** | 429889  |
| all fields (macro avg.)     | 80.3      | 88.66     | 79.68     | 429889  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 77.28     | 75.4      | 76.33     | 63265   |
| date                        | 94.9      | 91.62     | 93.23     | 63662   |
| first_author                | 92.55     | 90.17     | 91.35     | 63265   |
| inTitle                     | 95.02     | 93.26     | 94.13     | 63213   |
| issue                       | 1.58      | 81.25     | 3.09      | 16      |
| page                        | 95.14     | 93.32     | 94.23     | 53375   |
| title                       | 95.17     | 94.3      | 94.73     | 62044   |
| volume                      | 97.08     | 96.22     | 96.64     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.2**  | **90.51** | **91.35** | 429889  |
| all fields (macro avg.)     | 81.09     | 89.44     | 80.47     | 429889  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 91.16     | 88.94     | 90.04     | 63265   |
| date                        | 94.9      | 91.62     | 93.23     | 63662   |
| first_author                | 92.98     | 90.59     | 91.77     | 63265   |
| inTitle                     | 95.35     | 93.58     | 94.46     | 63213   |
| issue                       | 1.58      | 81.25     | 3.09      | 16      |
| page                        | 95.14     | 93.32     | 94.23     | 53375   |
| title                       | 97.34     | 96.45     | 96.89     | 62044   |
| volume                      | 97.08     | 96.22     | 96.64     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **94.65** | **92.92** | **93.78** | 429889  |
| all fields (macro avg.)     | 83.19     | 91.5      | 82.54     | 429889  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 84.33     | 82.28     | 83.29     | 63265   |
| date                        | 94.9      | 91.62     | 93.23     | 63662   |
| first_author                | 92.49     | 90.11     | 91.29     | 63265   |
| inTitle                     | 95.04     | 93.28     | 94.15     | 63213   |
| issue                       | 1.58      | 81.25     | 3.09      | 16      |
| page                        | 95.14     | 93.32     | 94.23     | 53375   |
| title                       | 97.08     | 96.19     | 96.63     | 62044   |
| volume                      | 97.08     | 96.22     | 96.64     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.5**  | **91.79** | **92.64** | 429889  |
| all fields (macro avg.)     | 82.2      | 90.53     | 81.57     | 429889  |

#### Instance-level results

```
Total expected instances: 		63664
Total extracted instances: 		68033
Total correct instances: 		40160 (strict) 
Total correct instances: 		42811 (soft) 
Total correct instances: 		50132 (Levenshtein) 
Total correct instances: 		46886 (RatcliffObershelp) 

Instance-level precision:	59.03 (strict) 
Instance-level precision:	62.93 (soft) 
Instance-level precision:	73.69 (Levenshtein) 
Instance-level precision:	68.92 (RatcliffObershelp) 

Instance-level recall:	63.08	(strict) 
Instance-level recall:	67.25	(soft) 
Instance-level recall:	78.74	(Levenshtein) 
Instance-level recall:	73.65	(RatcliffObershelp) 

Instance-level f-score:	60.99 (strict) 
Instance-level f-score:	65.01 (soft) 
Instance-level f-score:	76.13 (Levenshtein) 
Instance-level f-score:	71.2 (RatcliffObershelp) 

Matching 1 :	57007

Matching 2 :	1056

Matching 3 :	1499

Matching 4 :	732

Total matches :	60294
```

#### Citation context resolution

```

Total expected references: 	 63664 - 64.7 references per article
Total predicted references: 	 68033 - 69.14 references per article

Total expected citation contexts: 	 109022 - 110.79 citation contexts per article
Total predicted citation contexts: 	 98788 - 100.39 citation contexts per article

Total correct predicted citation contexts: 	 93711 - 95.23 citation contexts per article
Total wrong predicted citation contexts: 	 5077 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 94.86
Recall citation contexts: 	 85.96
fscore citation contexts: 	 90.19
```

## Fulltext structures

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from
the actual PDF content and are can be inconsistent from one document to another. The scores of the following metrics are
thus not very meaningful in absolute term, in particular for the strict matching (textual content of the srtructure can
be very long). As relative values for comparing different models, they seem however useful.

Evaluation on 984 random PDF files out of 982 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall   | f1        | support |
|-----------------------------|-----------|----------|-----------|---------|
| availability_stmt           | 29.21     | 27.86    | 28.52     | 585     |
| figure_title                | 0.07      | 0.02     | 0.03      | 31718   |
| funding_stmt                | 7.84      | 32.36    | 12.62     | 921     |
| reference_citation          | 56.96     | 55.81    | 56.38     | 108949  |
| reference_figure            | 58.4      | 51.02    | 54.46     | 68926   |
| reference_table             | 71.67     | 73.41    | 72.53     | 2381    |
| section_title               | 82.82     | 77.26    | 79.94     | 21831   |
| table_title                 | 0         | 0        | 0         | 1925    |
|                             |           |          |           |         |
| **all fields (micro avg.)** | **56.13** | **48.5** | **52.04** | 237236  |
| all fields (macro avg.)     | 38.37     | 39.72    | 38.06     | 237236  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall   | f1        | support |
|-----------------------------|-----------|----------|-----------|---------|
| availability_stmt           | 40.5      | 38.63    | 39.55     | 585     |
| figure_title                | 49.52     | 16.04    | 24.24     | 31718   |
| funding_stmt                | 7.84      | 32.36    | 12.62     | 921     |
| reference_citation          | 93.51     | 91.62    | 92.56     | 108949  |
| reference_figure            | 58.69     | 51.27    | 54.73     | 68926   |
| reference_table             | 71.79     | 73.54    | 72.66     | 2381    |
| section_title               | 83.86     | 78.23    | 80.94     | 21831   |
| table_title                 | 94.25     | 28.1     | 43.3      | 1925    |
|                             |           |          |           |         |
| **all fields (micro avg.)** | **78.13** | **67.5** | **72.43** | 237236  |
| all fields (macro avg.)     | 62.49     | 51.22    | 52.57     | 237236  |

**Document-level ratio results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 96.88     | 95.38     | 96.12     | 585     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **96.88** | **95.38** | **96.12** | 585     |
| all fields (macro avg.)     | 96.88     | 95.38     | 96.12     | 585     |

Evaluation metrics produced in 1355.49 seconds

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
| abstract                    | 9.53      | 9.25      | 9.39      | 984     |
| authors                     | 57.16     | 56.46     | 56.81     | 983     |
| first_author                | 89.39     | 88.39     | 88.89     | 982     |
| title                       | 83.33     | 81.3      | 82.3      | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **59.99** | **58.84** | **59.41** | 3933    |
| all fields (macro avg.)     | 59.85     | 58.85     | 59.35     | 3933    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 22.3      | 21.65     | 21.97     | 984     |
| authors                     | 57.47     | 56.77     | 57.11     | 983     |
| first_author                | 89.39     | 88.39     | 88.89     | 982     |
| title                       | 93.65     | 91.36     | 92.49     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **65.8**  | **64.53** | **65.16** | 3933    |
| all fields (macro avg.)     | 65.7      | 64.54     | 65.12     | 3933    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 47.43     | 46.04     | 46.73     | 984     |
| authors                     | 83.32     | 82.3      | 82.8      | 983     |
| first_author                | 90.01     | 89        | 89.5      | 982     |
| title                       | 96.15     | 93.8      | 94.96     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **79.31** | **77.78** | **78.54** | 3933    |
| all fields (macro avg.)     | 79.23     | 77.78     | 78.5      | 3933    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 44.5      | 43.19     | 43.84     | 984     |
| authors                     | 67.97     | 67.14     | 67.55     | 983     |
| first_author                | 89.39     | 88.39     | 88.89     | 982     |
| title                       | 95.73     | 93.39     | 94.55     | 984     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **74.46** | **73.02** | **73.74** | 3933    |
| all fields (macro avg.)     | 74.4      | 73.03     | 73.71     | 3933    |

#### Instance-level results

```
Total expected instances: 	984
Total correct instances: 	36 (strict) 
Total correct instances: 	108 (soft) 
Total correct instances: 	332 (Levenshtein) 
Total correct instances: 	260 (ObservedRatcliffObershelp) 

Instance-level recall:	3.66	(strict) 
Instance-level recall:	10.98	(soft) 
Instance-level recall:	33.74	(Levenshtein) 
Instance-level recall:	26.42	(RatcliffObershelp) 
```

## Citation metadata

Evaluation on 984 random PDF files out of 982 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 79.44     | 78.37     | 78.9      | 63265   |
| date                        | 95.89     | 94.2      | 95.04     | 63662   |
| first_author                | 94.82     | 93.51     | 94.16     | 63265   |
| inTitle                     | 95.81     | 94.88     | 95.34     | 63213   |
| issue                       | 2.01      | 75        | 3.91      | 16      |
| page                        | 96.28     | 95.45     | 95.86     | 53375   |
| title                       | 90.28     | 90.89     | 90.58     | 62044   |
| volume                      | 97.88     | 98.41     | 98.14     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.7**  | **92.14** | **92.42** | 429889  |
| all fields (macro avg.)     | 81.55     | 90.09     | 81.49     | 429889  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 79.57     | 78.5      | 79.04     | 63265   |
| date                        | 95.89     | 94.2      | 95.04     | 63662   |
| first_author                | 94.9      | 93.59     | 94.24     | 63265   |
| inTitle                     | 96.29     | 95.36     | 95.82     | 63213   |
| issue                       | 2.01      | 75        | 3.91      | 16      |
| page                        | 96.28     | 95.45     | 95.86     | 53375   |
| title                       | 95.95     | 96.59     | 96.27     | 62044   |
| volume                      | 97.88     | 98.41     | 98.14     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.63** | **93.07** | **93.35** | 429889  |
| all fields (macro avg.)     | 82.35     | 90.89     | 82.29     | 429889  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 93.31     | 92.06     | 92.68     | 63265   |
| date                        | 95.89     | 94.2      | 95.04     | 63662   |
| first_author                | 95.35     | 94.03     | 94.68     | 63265   |
| inTitle                     | 96.62     | 95.68     | 96.15     | 63213   |
| issue                       | 2.01      | 75        | 3.91      | 16      |
| page                        | 96.28     | 95.45     | 95.86     | 53375   |
| title                       | 97.69     | 98.34     | 98.01     | 62044   |
| volume                      | 97.88     | 98.41     | 98.14     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **96.01** | **95.43** | **95.72** | 429889  |
| all fields (macro avg.)     | 84.38     | 92.9      | 84.31     | 429889  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 86.75     | 85.59     | 86.17     | 63265   |
| date                        | 95.89     | 94.2      | 95.04     | 63662   |
| first_author                | 94.83     | 93.53     | 94.18     | 63265   |
| inTitle                     | 96.29     | 95.36     | 95.82     | 63213   |
| issue                       | 2.01      | 75        | 3.91      | 16      |
| page                        | 96.28     | 95.45     | 95.86     | 53375   |
| title                       | 97.53     | 98.19     | 97.86     | 62044   |
| volume                      | 97.88     | 98.41     | 98.14     | 61049   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **94.9**  | **94.33** | **94.62** | 429889  |
| all fields (macro avg.)     | 83.43     | 91.96     | 83.37     | 429889  |

#### Instance-level results

```
Total expected instances: 		63664
Total extracted instances: 		66161
Total correct instances: 		42407 (strict) 
Total correct instances: 		45253 (soft) 
Total correct instances: 		52913 (Levenshtein) 
Total correct instances: 		49509 (RatcliffObershelp) 

Instance-level precision:	64.1 (strict) 
Instance-level precision:	68.4 (soft) 
Instance-level precision:	79.98 (Levenshtein) 
Instance-level precision:	74.83 (RatcliffObershelp) 

Instance-level recall:	66.61	(strict) 
Instance-level recall:	71.08	(soft) 
Instance-level recall:	83.11	(Levenshtein) 
Instance-level recall:	77.77	(RatcliffObershelp) 

Instance-level f-score:	65.33 (strict) 
Instance-level f-score:	69.71 (soft) 
Instance-level f-score:	81.51 (Levenshtein) 
Instance-level f-score:	76.27 (RatcliffObershelp) 

Matching 1 :	58724

Matching 2 :	1019

Matching 3 :	1250

Matching 4 :	367

Total matches :	61360
```

#### Citation context resolution

```

Total expected references: 	 63664 - 64.7 references per article
Total predicted references: 	 66161 - 67.24 references per article

Total expected citation contexts: 	 109022 - 110.79 citation contexts per article
Total predicted citation contexts: 	 99932 - 101.56 citation contexts per article

Total correct predicted citation contexts: 	 96236 - 97.8 citation contexts per article
Total wrong predicted citation contexts: 	 3696 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 96.3
Recall citation contexts: 	 88.27
fscore citation contexts: 	 92.11
```

## Fulltext structures

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from
the actual PDF content and are can be inconsistent from one document to another. The scores of the following metrics are
thus not very meaningful in absolute term, in particular for the strict matching (textual content of the srtructure can
be very long). As relative values for comparing different models, they seem however useful.

Evaluation on 984 random PDF files out of 982 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 29.08     | 27.69     | 28.37     | 585     |
| figure_title                | 0.07      | 0.02      | 0.03      | 31718   |
| funding_stmt                | 6.15      | 29.97     | 10.2      | 921     |
| reference_citation          | 57.08     | 55.95     | 56.51     | 108949  |
| reference_figure            | 58.42     | 51.01     | 54.47     | 68926   |
| reference_table             | 71.56     | 73.46     | 72.5      | 2381    |
| section_title               | 82.83     | 77.26     | 79.95     | 21831   |
| table_title                 | 0         | 0         | 0         | 1925    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **56.02** | **48.55** | **52.02** | 237236  |
| all fields (macro avg.)     | 38.15     | 39.42     | 37.75     | 237236  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 40.39     | 38.46     | 39.4      | 585     |
| figure_title                | 49.72     | 15.98     | 24.19     | 31718   |
| funding_stmt                | 6.15      | 29.97     | 10.2      | 921     |
| reference_citation          | 93.65     | 91.8      | 92.72     | 108949  |
| reference_figure            | 58.7      | 51.26     | 54.73     | 68926   |
| reference_table             | 71.64     | 73.54     | 72.58     | 2381    |
| section_title               | 83.86     | 78.23     | 80.95     | 21831   |
| table_title                 | 94.43     | 28.16     | 43.38     | 1925    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **77.96** | **67.57** | **72.39** | 237236  |
| all fields (macro avg.)     | 62.32     | 50.92     | 52.27     | 237236  |

**Document-level ratio results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 96.87     | 95.21     | 96.03     | 585     |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **96.87** | **95.21** | **96.03** | 585     |
| all fields (macro avg.)     | 96.87     | 95.21     | 96.03     | 585     |

Evaluation metrics produced in 1309.47 seconds

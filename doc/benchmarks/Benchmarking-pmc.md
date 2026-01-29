# Benchmarking PubMed Central

## General

This is the end-to-end benchmarking result for GROBID version **0.8.2** against the `PMC_sample_1943` dataset, see
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

Evaluation on 1943 random PDF PMC files out of 1943 PDF from 1943 different journals (0 PDF parsing failure).

Runtime for processing 1943 PDF: **1467** seconds, (0.75s per PDF) on Ubuntu 22.04, 16 CPU (32 threads), 128GB RAM and
with a GeForce GTX 1080 Ti GPU.

Note: with CRF only models, runtime is 470s (0.24 seconds per PDF) with 4 CPU, 8 threads.

## Header metadata

Evaluation on 1943 random PDF files out of 1941 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 16.89     | 16.54     | 16.71     | 1911    |
| authors                     | 90.85     | 90.57     | 90.71     | 1941    |
| first_author                | 96.49     | 96.19     | 96.34     | 1941    |
| keywords                    | 65.58     | 63.91     | 64.73     | 1380    |
| title                       | 83.83     | 83.22     | 83.52     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **71.44** | **70.65** | **71.04** | 9116    |
| all fields (macro avg.)     | 70.73     | 70.09     | 70.4      | 9116    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 63.98     | 62.64     | 63.3      | 1911    |
| authors                     | 92.66     | 92.38     | 92.52     | 1941    |
| first_author                | 96.9      | 96.6      | 96.75     | 1941    |
| keywords                    | 74.2      | 72.32     | 73.25     | 1380    |
| title                       | 91.45     | 90.79     | 91.12     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **84.6**  | **83.67** | **84.13** | 9116    |
| all fields (macro avg.)     | 83.84     | 82.94     | 83.39     | 9116    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 90.86     | 88.96     | 89.9      | 1911    |
| authors                     | 95.66     | 95.36     | 95.51     | 1941    |
| first_author                | 97.16     | 96.86     | 97.01     | 1941    |
| keywords                    | 84.61     | 82.46     | 83.52     | 1380    |
| title                       | 97.77     | 97.07     | 97.42     | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.79** | **92.75** | **93.27** | 9116    |
| all fields (macro avg.)     | 93.21     | 92.14     | 92.67     | 9116    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 87.07     | 85.24     | 86.14     | 1911    |
| authors                     | 94.11     | 93.82     | 93.96     | 1941    |
| first_author                | 96.49     | 96.19     | 96.34     | 1941    |
| keywords                    | 79.93     | 77.9      | 78.9      | 1380    |
| title                       | 95.75     | 95.06     | 95.4      | 1943    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **91.39** | **90.38** | **90.88** | 9116    |
| all fields (macro avg.)     | 90.67     | 89.64     | 90.15     | 9116    |

#### Instance-level results

```
Total expected instances: 	1943
Total correct instances: 	215 (strict) 
Total correct instances: 	888 (soft) 
Total correct instances: 	1421 (Levenshtein) 
Total correct instances: 	1272 (ObservedRatcliffObershelp) 

Instance-level recall:	11.07	(strict) 
Instance-level recall:	45.7	(soft) 
Instance-level recall:	73.13	(Levenshtein) 
Instance-level recall:	65.47	(RatcliffObershelp) 
```

## Citation metadata

Evaluation on 1943 random PDF files out of 1941 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 83.1      | 75.94     | 79.36     | 85778   |
| date                        | 94.7      | 83.83     | 88.93     | 87067   |
| first_author                | 89.85     | 82.09     | 85.79     | 85778   |
| inTitle                     | 73.27     | 71.46     | 72.35     | 81007   |
| issue                       | 91.43     | 87.44     | 89.39     | 16635   |
| page                        | 94.69     | 83.31     | 88.63     | 80501   |
| title                       | 79.78     | 74.96     | 77.29     | 80736   |
| volume                      | 96.17     | 89.37     | 92.64     | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **87.32** | **80.34** | **83.69** | 597569  |
| all fields (macro avg.)     | 87.88     | 81.05     | 84.3      | 597569  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 83.57     | 76.37     | 79.81     | 85778   |
| date                        | 94.7      | 83.83     | 88.93     | 87067   |
| first_author                | 90.02     | 82.24     | 85.96     | 85778   |
| inTitle                     | 85.04     | 82.93     | 83.97     | 81007   |
| issue                       | 91.43     | 87.44     | 89.39     | 16635   |
| page                        | 94.69     | 83.31     | 88.63     | 80501   |
| title                       | 91.55     | 86.01     | 88.7      | 80736   |
| volume                      | 96.17     | 89.37     | 92.64     | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **90.73** | **83.48** | **86.95** | 597569  |
| all fields (macro avg.)     | 90.9      | 83.94     | 87.25     | 597569  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 89.28     | 81.59     | 85.26     | 85778   |
| date                        | 94.7      | 83.83     | 88.93     | 87067   |
| first_author                | 90.24     | 82.44     | 86.17     | 85778   |
| inTitle                     | 86.28     | 84.14     | 85.2      | 81007   |
| issue                       | 91.43     | 87.44     | 89.39     | 16635   |
| page                        | 94.69     | 83.31     | 88.63     | 80501   |
| title                       | 93.91     | 88.22     | 90.98     | 80736   |
| volume                      | 96.17     | 89.37     | 92.64     | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.08** | **84.72** | **88.24** | 597569  |
| all fields (macro avg.)     | 92.09     | 85.04     | 88.4      | 597569  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 86.05     | 78.63     | 82.17     | 85778   |
| date                        | 94.7      | 83.83     | 88.93     | 87067   |
| first_author                | 89.87     | 82.1      | 85.81     | 85778   |
| inTitle                     | 83.6      | 81.53     | 82.55     | 81007   |
| issue                       | 91.43     | 87.44     | 89.39     | 16635   |
| page                        | 94.69     | 83.31     | 88.63     | 80501   |
| title                       | 93.51     | 87.85     | 90.59     | 80736   |
| volume                      | 96.17     | 89.37     | 92.64     | 80067   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **91.12** | **83.84** | **87.33** | 597569  |
| all fields (macro avg.)     | 91.25     | 84.26     | 87.59     | 597569  |

#### Instance-level results

```
Total expected instances: 		90125
Total extracted instances: 		85138
Total correct instances: 		38530 (strict) 
Total correct instances: 		50629 (soft) 
Total correct instances: 		55467 (Levenshtein) 
Total correct instances: 		52029 (RatcliffObershelp) 

Instance-level precision:	45.26 (strict) 
Instance-level precision:	59.47 (soft) 
Instance-level precision:	65.15 (Levenshtein) 
Instance-level precision:	61.11 (RatcliffObershelp) 

Instance-level recall:	42.75	(strict) 
Instance-level recall:	56.18	(soft) 
Instance-level recall:	61.54	(Levenshtein) 
Instance-level recall:	57.73	(RatcliffObershelp) 

Instance-level f-score:	43.97 (strict) 
Instance-level f-score:	57.77 (soft) 
Instance-level f-score:	63.3 (Levenshtein) 
Instance-level f-score:	59.37 (RatcliffObershelp) 

Matching 1 :	67992

Matching 2 :	4122

Matching 3 :	1870

Matching 4 :	663

Total matches :	74647
```

#### Citation context resolution

```

Total expected references: 	 90125 - 46.38 references per article
Total predicted references: 	 85138 - 43.82 references per article

Total expected citation contexts: 	 139835 - 71.97 citation contexts per article
Total predicted citation contexts: 	 114503 - 58.93 citation contexts per article

Total correct predicted citation contexts: 	 96979 - 49.91 citation contexts per article
Total wrong predicted citation contexts: 	 17524 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 84.7
Recall citation contexts: 	 69.35
fscore citation contexts: 	 76.26
```

## Fulltext structures

Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from
the actual PDF content and are can be inconsistent from one document to another. The scores of the following metrics are
thus not very meaningful in absolute term, in particular for the strict matching (textual content of the srtructure can
be very long). As relative values for comparing different models, they seem however useful.

Evaluation on 1943 random PDF files out of 1941 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall   | f1        | support |
|-----------------------------|-----------|----------|-----------|---------|
| figure_title                | 31.52     | 26.53    | 28.81     | 7281    |
| reference_citation          | 58.13     | 58.76    | 58.44     | 134196  |
| reference_figure            | 60.6      | 68.27    | 64.21     | 19330   |
| reference_table             | 82.87     | 89.52    | 86.06     | 7327    |
| section_title               | 73.59     | 67.77    | 70.56     | 27619   |
| table_title                 | 67.76     | 49.58    | 57.26     | 3971    |
|                             |           |          |           |         |
| **all fields (micro avg.)** | **60.67** | **60.7** | **60.69** | 199724  |
| all fields (macro avg.)     | 62.41     | 60.07    | 60.89     | 199724  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| figure_title                | 79.54     | 66.97     | 72.72     | 7281    |
| reference_citation          | 62.41     | 63.08     | 62.75     | 134196  |
| reference_figure            | 61.1      | 68.84     | 64.74     | 19330   |
| reference_table             | 83.04     | 89.71     | 86.25     | 7327    |
| section_title               | 79.09     | 72.84     | 75.84     | 27619   |
| table_title                 | 94.22     | 68.95     | 79.63     | 3971    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **66.2**  | **66.22** | **66.21** | 199724  |
| all fields (macro avg.)     | 76.57     | 71.73     | 73.65     | 199724  |

**Document-level ratio results**

| label                       | precision | recall | f1    | support |
|-----------------------------|-----------|--------|-------|---------|
|                             |           |        |       |         |
| **all fields (micro avg.)** | **0**     | **0**  | **0** | 0       |
| all fields (macro avg.)     | 0         | 0      | 0     | 0       |

Evaluation metrics produced in 1247.994 seconds


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
| abstract                    | 2.42      | 2.36      | 2.39      | 1989    |
| authors                     | 85.36     | 84.63     | 85        | 1998    |
| first_author                | 97.02     | 96.29     | 96.66     | 1996    |
| keywords                    | 58.08     | 59.19     | 58.63     | 838     |
| title                       | 77.47     | 76.74     | 77.1      | 1999    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **65.1**  | **64.51** | **64.81** | 8820    |
| all fields (macro avg.)     | 64.07     | 63.84     | 63.96     | 8820    |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1       | support |
|-----------------------------|-----------|-----------|----------|---------|
| abstract                    | 60.6      | 59.23     | 59.9     | 1989    |
| authors                     | 85.76     | 85.04     | 85.4     | 1998    |
| first_author                | 97.22     | 96.49     | 96.86    | 1996    |
| keywords                    | 63.23     | 64.44     | 63.83    | 838     |
| title                       | 79.6      | 78.84     | 79.22    | 1999    |
|                             |           |           |          |         |
| **all fields (micro avg.)** | **79.16** | **78.45** | **78.8** | 8820    |
| all fields (macro avg.)     | 77.28     | 76.81     | 77.04    | 8820    |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 80.71     | 78.88     | 79.79     | 1989    |
| authors                     | 92.88     | 92.09     | 92.49     | 1998    |
| first_author                | 97.53     | 96.79     | 97.16     | 1996    |
| keywords                    | 79.27     | 80.79     | 80.02     | 838     |
| title                       | 92.12     | 91.25     | 91.68     | 1999    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.73** | **88.91** | **89.32** | 8820    |
| all fields (macro avg.)     | 88.5      | 87.96     | 88.23     | 8820    |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| abstract                    | 77.21     | 75.47     | 76.33     | 1989    |
| authors                     | 88.89     | 88.14     | 88.51     | 1998    |
| first_author                | 97.02     | 96.29     | 96.66     | 1996    |
| keywords                    | 70.96     | 72.32     | 71.63     | 838     |
| title                       | 87.78     | 86.94     | 87.36     | 1999    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **86.13** | **85.35** | **85.74** | 8820    |
| all fields (macro avg.)     | 84.37     | 83.83     | 84.1      | 8820    |

#### Instance-level results

```
Total expected instances: 	1999
Total correct instances: 	40 (strict) 
Total correct instances: 	728 (soft) 
Total correct instances: 	1238 (Levenshtein) 
Total correct instances: 	1066 (ObservedRatcliffObershelp) 

Instance-level recall:	2	(strict) 
Instance-level recall:	36.42	(soft) 
Instance-level recall:	61.93	(Levenshtein) 
Instance-level recall:	53.33	(RatcliffObershelp) 
```

## Citation metadata

Evaluation on 2000 random PDF files out of 1998 PDF (ratio 1.0).

#### Strict Matching (exact matches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 88.19     | 83.11     | 85.58     | 97132   |
| date                        | 91.7      | 86.15     | 88.84     | 97579   |
| doi                         | 70.86     | 83.85     | 76.81     | 16894   |
| first_author                | 95.08     | 89.53     | 92.22     | 97132   |
| inTitle                     | 82.88     | 79.3      | 81.05     | 96379   |
| issue                       | 94.35     | 91.93     | 93.13     | 30312   |
| page                        | 94.99     | 78.22     | 85.8      | 88551   |
| pmcid                       | 66.44     | 86.12     | 75.01     | 807     |
| pmid                        | 69.99     | 84.57     | 76.59     | 2093    |
| title                       | 84.89     | 83.41     | 84.14     | 92415   |
| volume                      | 96.27     | 95.06     | 95.66     | 87661   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **89.87** | **85.21** | **87.48** | 706955  |
| all fields (macro avg.)     | 85.06     | 85.57     | 84.98     | 706955  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 89.35     | 84.2      | 86.7      | 97132   |
| date                        | 91.7      | 86.15     | 88.84     | 97579   |
| doi                         | 75.34     | 89.16     | 81.67     | 16894   |
| first_author                | 95.51     | 89.93     | 92.64     | 97132   |
| inTitle                     | 92.37     | 88.37     | 90.33     | 96379   |
| issue                       | 94.35     | 91.93     | 93.13     | 30312   |
| page                        | 94.99     | 78.22     | 85.8      | 88551   |
| pmcid                       | 75.72     | 98.14     | 85.48     | 807     |
| pmid                        | 74.42     | 89.92     | 81.44     | 2093    |
| title                       | 93.24     | 91.61     | 92.42     | 92415   |
| volume                      | 96.27     | 95.06     | 95.66     | 87661   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **92.69** | **87.88** | **90.22** | 706955  |
| all fields (macro avg.)     | 88.48     | 89.34     | 88.55     | 706955  |

#### Levenshtein Matching (Minimum Levenshtein distance at 0.8)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 94.61     | 89.16     | 91.8      | 97132   |
| date                        | 91.7      | 86.15     | 88.84     | 97579   |
| doi                         | 77.58     | 91.81     | 84.1      | 16894   |
| first_author                | 95.66     | 90.07     | 92.78     | 97132   |
| inTitle                     | 93.35     | 89.31     | 91.29     | 96379   |
| issue                       | 94.35     | 91.93     | 93.13     | 30312   |
| page                        | 94.99     | 78.22     | 85.8      | 88551   |
| pmcid                       | 75.72     | 98.14     | 85.48     | 807     |
| pmid                        | 74.42     | 89.92     | 81.44     | 2093    |
| title                       | 96.08     | 94.41     | 95.24     | 92415   |
| volume                      | 96.27     | 95.06     | 95.66     | 87661   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **94.01** | **89.13** | **91.51** | 706955  |
| all fields (macro avg.)     | 89.52     | 90.38     | 89.6      | 706955  |

#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at 0.95)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| authors                     | 91.56     | 86.29     | 88.85     | 97132   |
| date                        | 91.7      | 86.15     | 88.84     | 97579   |
| doi                         | 76.04     | 89.98     | 82.42     | 16894   |
| first_author                | 95.13     | 89.58     | 92.27     | 97132   |
| inTitle                     | 91.12     | 87.18     | 89.1      | 96379   |
| issue                       | 94.35     | 91.93     | 93.13     | 30312   |
| page                        | 94.99     | 78.22     | 85.8      | 88551   |
| pmcid                       | 66.44     | 86.12     | 75.01     | 807     |
| pmid                        | 69.99     | 84.57     | 76.59     | 2093    |
| title                       | 95.41     | 93.74     | 94.57     | 92415   |
| volume                      | 96.27     | 95.06     | 95.66     | 87661   |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **93.05** | **88.22** | **90.57** | 706955  |
| all fields (macro avg.)     | 87.55     | 88.07     | 87.48     | 706955  |

#### Instance-level results

```
Total expected instances: 		98748
Total extracted instances: 		97758
Total correct instances: 		43658 (strict) 
Total correct instances: 		54646 (soft) 
Total correct instances: 		58827 (Levenshtein) 
Total correct instances: 		55558 (RatcliffObershelp) 

Instance-level precision:	44.66 (strict) 
Instance-level precision:	55.9 (soft) 
Instance-level precision:	60.18 (Levenshtein) 
Instance-level precision:	56.83 (RatcliffObershelp) 

Instance-level recall:	44.21	(strict) 
Instance-level recall:	55.34	(soft) 
Instance-level recall:	59.57	(Levenshtein) 
Instance-level recall:	56.26	(RatcliffObershelp) 

Instance-level f-score:	44.43 (strict) 
Instance-level f-score:	55.62 (soft) 
Instance-level f-score:	59.87 (Levenshtein) 
Instance-level f-score:	56.55 (RatcliffObershelp) 

Matching 1 :	79095

Matching 2 :	4449

Matching 3 :	4361

Matching 4 :	2101

Total matches :	90006
```

#### Citation context resolution

```

Total expected references: 	 98746 - 49.37 references per article
Total predicted references: 	 97758 - 48.88 references per article

Total expected citation contexts: 	 142776 - 71.39 citation contexts per article
Total predicted citation contexts: 	 134412 - 67.21 citation contexts per article

Total correct predicted citation contexts: 	 115887 - 57.94 citation contexts per article
Total wrong predicted citation contexts: 	 18525 (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)

Precision citation contexts: 	 86.22
Recall citation contexts: 	 81.17
fscore citation contexts: 	 83.62
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
| availability_stmt           | 28.91     | 24.89     | 26.75     | 446     |
| figure_title                | 4.3       | 2.34      | 3.03      | 22967   |
| funding_stmt                | 3.48      | 23.03     | 6.05      | 747     |
| reference_citation          | 72.03     | 70.95     | 71.49     | 147384  |
| reference_figure            | 70.38     | 77.12     | 73.6      | 47896   |
| reference_table             | 45.65     | 86.74     | 59.82     | 5957    |
| section_title               | 71.34     | 69.92     | 70.62     | 32368   |
| table_title                 | 7.41      | 2.7       | 3.96      | 3925    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **65.48** | **65.05** | **65.26** | 261690  |
| all fields (macro avg.)     | 37.94     | 44.71     | 39.41     | 261690  |

#### Soft Matching (ignoring punctuation, case and space characters mismatches)

**Field-level results**

| label                       | precision | recall    | f1        | support |
|-----------------------------|-----------|-----------|-----------|---------|
| availability_stmt           | 50.78     | 43.72     | 46.99     | 446     |
| figure_title                | 68.34     | 37.27     | 48.23     | 22967   |
| funding_stmt                | 3.71      | 24.5      | 6.44      | 747     |
| reference_citation          | 84.33     | 83.07     | 83.7      | 147384  |
| reference_figure            | 71.02     | 77.81     | 74.26     | 47896   |
| reference_table             | 46.07     | 87.53     | 60.36     | 5957    |
| section_title               | 76.9      | 75.37     | 76.13     | 32368   |
| table_title                 | 82.8      | 30.17     | 44.22     | 3925    |
|                             |           |           |           |         |
| **all fields (micro avg.)** | **76.71** | **76.21** | **76.46** | 261690  |
| all fields (macro avg.)     | 60.49     | 57.43     | 55.04     | 261690  |

**Document-level ratio results**

| label                       | precision | recall   | f1        | support |
|-----------------------------|-----------|----------|-----------|---------|
| availability_stmt           | 84.96     | 86.1     | 85.52     | 446     |
|                             |           |          |           |         |
| **all fields (micro avg.)** | **84.96** | **86.1** | **85.52** | 446     |
| all fields (macro avg.)     | 84.96     | 86.1     | 85.52     | 446     |

Evaluation metrics produced in 1599.065 seconds



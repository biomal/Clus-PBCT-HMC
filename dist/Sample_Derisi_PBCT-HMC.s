[General]
Compatibility = MLJ08

[Data]
File = derisi_FUN.trainvalid.arff
TestSet = derisi_FUN.test.arff

[Hierarchical]
Type = TREE
HSeparator = /
WType = ExpAvgParentWeight

[Attributes]
Target = 64-562
Weights = 1

[Tree]
FTest = 0.125
PBCT = Yes

[Model]
MinimalWeight = 5.0

[Output]
OutputMultiLabelErrors = True

# Clus PBCT-HMC

This is the version of the PBCT-HMC implemented in [Clus software](http://clus.sourceforge.net/doku.php).

Please refer to publication ["Predictive Bi-Clustering Trees for Hierarchical Multi-label Classification"](TBD). Authors: 
1. Bruna Zamith Santos, Universidade Federal de São Carlos
2. Felipe Kenji Nakano, Katholieke Universiteit Leuven
3. Ricardo Cerri, Universidade Federal de São Carlos
4. Celine Vens, Katholieke Universiteit Leuven

## Running 
To execute Clus PBCT-HMC, run:
```
cd dist/
java -jar Clus_PBCT-HMC.jar YourSettingsFile.s
```

A sample settings file can be found in the same ``dist/`` folder.

If you are used to regular Clus settings files, just include the following parameter:

``` 
[Hierarchical]
Type = TREE
HSeparator = /
WType = ExpAvgParentWeight

[Tree]
PBCT = Yes
``` 

Keep in mind that you need to input a multi-target regression dataset:
``` 
[Attributes]
Target = 64-562
Weights = 1


[Output]
OutputMultiLabelErrors = True
``` 

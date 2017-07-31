# tensorflow for android demo
forked from tensorflow.
edit some buildfile and C lib to enable build on android

the model is trained with r1.1, the lib used to build android lib is r1.3 (the model can not be restored with r1.3 in python, so you have to do this with r1.1, but it can run on r1.3 based android lib)

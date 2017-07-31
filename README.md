# tensorflow for android demo
forked from tensorflow.

edit some BUILD file and C lib to enable build on android

this android demo run my own keyword spotting with tensorflow on android, where the keyword is 你好乐乐

I don't want to modify the whole project structure because of the complicated BUILD file and dependency hierarchy.

The android project in tensorflow/tensorflow/example/android

the model is trained with r1.1, the lib used to build android lib is r1.3 (the model can not be restored with r1.3 in python, so you have to do this with r1.1, but it can run on r1.3 based android lib)



the project is build with bazel

build cmd:
`bazel build -c opt //tensorflow/examples/android:tensorflow_demo --define ANDROID_TYPES=__ANDROID_TYPES_FULL__`

install cmd:
`adb install -r bazel-bin/tensorflow/examples/android/tensorflow_demo.apk`

referenced: [https://github.com/tensorflow/tensorflow/tree/master/tensorflow/examples/android]()


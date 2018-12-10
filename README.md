# JPEGOptimizerLib
This is a fork from the original project by https://collicalex.github.io/JPEGOptimizer/ so the optimization algorithm can be easely integrated as a stand-alone library in other java projects. 
Just by adding the two classes ImageUltils.java and JPEGFiles.java to your project, optimization can be performed like this:

<code>
JPEGFiles jpegFile = new JPEGFiles(myFile);
   try {
        jpegFile.optimize(new File(file.getPath()), 0.75);
       } catch (IOException e) {
          logger.error("Optimization error: "+ myFile.getName());
       }
   logger.info("Optimiztion success");
  </code>

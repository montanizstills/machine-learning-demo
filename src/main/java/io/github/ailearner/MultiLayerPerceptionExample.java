package io.github.ailearner;

import io.github.ailearner.utils.FileHandler;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MultiLayerPerceptionExample {
    /**
     * Create our X-input and Y-output vectors for Training;
     *
     * @param file_path the file to read data from;
     * @return a Pair consisting of X,Y values;
     */
    INDArray[] prepareTrainingData(String file_path) {
        Integer blockSize = 3;
        INDArray context = Nd4j.zeros(1, blockSize); // very computationally expensive to know nRows up front;
        INDArray tensorX = Nd4j.zeros(1, blockSize);
        INDArray tensorY = Nd4j.zeros(1, 1);

        // create map of idx to alphanumeric char
        Map<String, Integer> charToIntegerAlphabet = IntStream
                .rangeClosed('a', 'z')
                .boxed() //.mapToObj(c -> c), difference?
                .collect(Collectors.toMap(
                        // not a fan of  this logic for map; not easily readable;
                        String::valueOf,
                        c -> c - 'a' + 1) // 'a' is ASCII value, subtract from c, convert to int, add 1.
                );
        charToIntegerAlphabet.put(".", 0);

        Map<Integer, String> integerToCharAlphabet = charToIntegerAlphabet
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        // read all words from names.txt
        FileHandler fh = new FileHandler();
        ArrayList allWords = new ArrayList<>();
        fh.readWordsFromFile(file_path).forEach(word -> {
            allWords.add(word);
            // add array of len(context) to X for each letter in "word", where: len(context) == blockSize
            String[] allLettersInAWord = word.toString().split("");
            Arrays.stream(allLettersInAWord).forEach(letter -> {
                System.out.println("The current letter under inspection is: " + letter);
                Integer idx = charToIntegerAlphabet.get(letter);  // not a fan of this impl;
                // context = null; // update context;
                System.out.println(idx);
                tensorX.add(context);
                tensorY.add(idx);
            });
        });
        System.out.println("X: " + tensorX);
        System.out.println("Y: " + tensorY);
        return null;
    }

    public static void main(String[] args) {
        MultiLayerPerceptionExample mlp = new MultiLayerPerceptionExample();
        mlp.prepareTrainingData(System.getProperty("user.dir") + "/src/main/java/resources/names.txt");
        System.exit(1);

        /**
         * Goal: For the given context we want to predict the next Y.
         *
         * Forward Pass:
         * 1. Take C[X] and embed (fully-connected) in Layer1 (*hyper parameter); Or, C[X[i]];
         *  Create a vector of embeddings:
         *              In Bengio et. 2003, the designers uses 3 word embeddings.
         *              (https://www.jmlr.org/papers/volume3/bengio03a/bengio03a.pdf)
         *              (https://github.com/karpathy/makemore/blob/master/makemore.py)
         *
         *              If we take all the data set (as potential samples of f(x)),
         *              Then the lookup matrix, C, looks like:
         *                  C.shape() = (len(all_chars_x)+'.', 1) or ('a,...,z,<.>', 1)
         *              And, the embedding vector for the given context looks like: C[X];
         *              If we want a specific char, say 'a', then the embedding vector looks like: C[X[i]] = C['a'];
         *
         *
         * 2. Take Layer2=tanh(Layer1), fully-connected (matrix) to C,
         *  Take Layer2=tanh(Layer1), fully-connected (matrix) to C for non-linearity;
         *
         *
         *
         * 3. Take Softmax(Layer2) -> tokenOut; Or, yield(): sample/expected output; Or, P(w_i | context);
         *
         *
         * **Note:
         *  Context comes in as stream. Y is the next expected char in the stream. Aka, MultiHeadAttention.
         *  Hyper Parameters: Train at different values to reduce loss;
         *  We use random vector inits to help avoid linearity.
         */

        Integer contextLength = 3; // amount of chars used to predict next one;
        Integer numberOfNeurons = 100; // we want to fully connect all first layer input neurons to this;
        Integer dimsToSqueeze = 2; // squeeze the first layer input to this dimension;

        // get all chars from the names.txt file;
        ArrayList allCharsX = null; //mlp.prepareTrainingData("/file/path/names.txt");

        // "first layer input" - input to model/neural-net
        // is a vectorX with dims: [len(all_chars_x), hyper_parameter]
        // and, should start with as zero tensor; then we will fill it with the context;

        INDArray vectorX = Nd4j.zeros(allCharsX.size(), dimsToSqueeze);

        // first layer output vector: in(contextLength) => out(numberOfSecondLayerInputNeurons)
        INDArray vectorY = Nd4j.rand(contextLength, numberOfNeurons);

        // embedding vector (first layer of our neural net)
        INDArray C = Nd4j.create(1);

        // get the embedding vector for the given context;
        // Nd4j.create(allCharsX.size()).putScalar(index, 1).mmul(C); //F.one_hot(vectorX, num_classes=len(all_chars_x)).mmuli(C);
        // INDArray emb_cX = vectorX.mmuli(C);
        INDArray emb_cX = C.get(vectorX);

        // second layer of our neural net, fully connected to C[X[i]];
        INDArray layer1 = Nd4j.rand(
                (int) vectorX.shape()[0],
                numberOfNeurons
        );
        INDArray layer2 = Nd4j.nn().tanh(layer1); // fully connected to C;

        // softmax output
        INDArray tokenOut = Nd4j.nn().softmax(layer2); // P(w_i | context);


    }

    void train() {
        //Prepare Training Data
        prepareTrainingData("file/path/names.txt");
        // this.run(training = true, sampleSize = 10000); // if training==true, sampleSize = epochs;
        // loss function and backpropagation
        // loss = Nd4j.nn().lossFunction(layer2, vectorY);
        // Nd4j.loss().softmaxCrossEntropy(null,vectorY, 0.1);
    }

    void sample(Integer sampleSize) {
        // Sample from the model
        // this.run(training = false, sampleSize); // if training == false, sampleSize = blockSize;
    }

}

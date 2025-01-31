package io.github.ailearner;

import io.github.ailearner.utils.FileHandler;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

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
        INDArray tensorX = Nd4j.zeros(1, blockSize);
        INDArray tensorY = Nd4j.zeros(1, 1);

        Map<String, Integer> charToIntegerAlphabet = IntStream.rangeClosed('a', 'z')
                .boxed() //.mapToObj(c -> c), difference?
                .collect(Collectors.toMap(
                        // not a fan of  this logic for map; not easily readable;
                        c -> String.valueOf((char) c.intValue()), c -> c - 'a' + 1) // 'a' is ASCII value, subtract from c, convert to int, add 1.
                );
        // create map of idx to alphanumeric char
        charToIntegerAlphabet.put(".", 0);

        Map<Integer, String> integerToCharAlphabet = charToIntegerAlphabet.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        // read all words from names.txt
        FileHandler fh = new FileHandler();


        fh.readWordsFromFile(file_path).forEach(word -> {
            System.out.println("=============");
            System.out.printf("Word: %s\n", word);

            // update X, Y: append updates to each matrix (X,Y) in same order (x1,y1) + (x2,y2).
            // add array of len(context) to X for each letter in "word", where: len(context) == blockSize
            // we want to stack [[0,0,0],[0,0,5],[0,5,13]...[13,1,0]], like: INDArray context = tensorX[1:]+[0, 0, idx]
            Arrays.stream(word.toString().split("")).forEach(letter -> {
                Integer idx = charToIntegerAlphabet.get(letter);  // not a fan of this impl;
                System.out.printf("Curr Y: %d, %s\n", idx, letter);

                // so lets start with a new context vector each iter;
                INDArray context = Nd4j.zeros(1, 3);
                INDArray slice = Nd4j.zeros(1, 3);

                // OPTION 1: update each elem individually, then add to TensorX
                // a.)
                // slice.putScalar(new int[]{0, 2}, idx); // third elem
                // slice.putScalar(new int[]{0, 1}, 0); // second elem
                // slice.putScalar(new int[]{0, 0}, 0); // first elem

                // b.)
                // **Note: addi() and addiRowVector produces same results, why? addiColumn vector columns of matrix shape mismatch;
                // Nd4j.vstack(tensorX,slice);


                // OPTION 2: first and second elem can be appended in one operation.
                // OR, slice.get(slice_index).put(val1,val2);

                // hacky, try more linearly algebraic solution
                // slice.get(NDArrayIndex.indices(-0)).getColumns(0, 1, 2);
                // slice.putScalar(new int[]{0, 2}, idx); // last elem
                // System.out.println(Nd4j.vstack(tensorX, slice));


                // OPTION 3: take slice of last row then dup and change dup val for positions: [0],[1],[2] -> [prev,prev,idx];
                slice = tensorX
                        .get(NDArrayIndex.indices(-1))
                        // hacky, bc we know the dataset, but prob a more elegant sol. exists for our case
                        // also need more prac with library
                        .get(NDArrayIndex.indices(1));
                //  INDArray tensorXSlice = tensorX.get(NDArrayIndex.point(tensorX.length() - 1), NDArrayIndex.all());
                // Nd4j.vstack(tensorX,slice);


                System.out.printf("TensorX: %s\n", tensorX);


            });
            System.exit(1);
        });
        return null;
    }

    public static void main(String[] args) {
        MultiLayerPerceptionExample mlp = new MultiLayerPerceptionExample();
        mlp.prepareTrainingData(System.getProperty("user.dir") + "/src/main/java/resources/names.txt");
    }


    void train() {
        //Prepare Training Data
        prepareTrainingData("file/path/names.txt");
        // this.run(training = true, sampleSize = 10000); // if training==true, sampleSize = epochs;
        // loss function and backpropagation
        // loss = Nd4j.nn().lossFunction(layer2, vectorY);
        // Nd4j.loss().softmaxCrossEntropy(null,vectorY, 0.1);

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
        INDArray layer1 = Nd4j.rand((int) vectorX.shape()[0], numberOfNeurons);
        INDArray layer2 = Nd4j.nn().tanh(layer1); // fully connected to C;

        // softmax output
        INDArray tokenOut = Nd4j.nn().softmax(layer2); // P(w_i | context);

        /**
         * Training:
         * We need to calc the Negative Log Likelihood. # Manual Impl
         * This is: the prob(x|i)/prob(x), i.e, prob of "x" follows "i" divided by gaussian prob of any letter follow "x".
         * This is easy in works in our case since we are only predicting the next English letter given some other English Letter.
         * Does not scale if {x: x in A} is a large array.
         * <p>
         * OR
         * <p>
         * We need to calc the cross_entropy. # Handles large inputs better
         * <p>
         * Backpropagation:
         * Do gradient descent calculus.
         * <p>
         * //Mini Batching??
         * <p>
         * //Sample
         * /*
         * * Start with seed for predictable results during testing and sampling; do not release into wild with seed.
         * * Forward Pass
         * * Return Sample
         */

    }

    void sample(Integer sampleSize) {
        // Sample from the model
        // this.run(training = false, sampleSize); // if training == false, sampleSize = blockSize;
    }

}

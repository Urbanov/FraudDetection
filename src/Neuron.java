import java.util.Random;

public abstract class Neuron {
    public static final double MOMENTUM = 0.07;
    public double output;


    private Network network;

    private double [] weights;
    private double [] exWeights;
    private double [] changes;

    // sum of products of input values from lower layer neurons and their edges' values
    private double inputSum;

    // parameter used during calculating weights changes in neural network
    private double delta;

    // number of lower layer inputs
    private int inputs;

    // number of a layer the neuron belongs to
    private int layer;

    // enabling use of an additional const input equal to 1
    private boolean enableConstAddend;

    /**
     *  Initializes all variables of a neuron.
     * @param pNetwork network neuron belongs to
     * @param numberOfInputs number of neurons in lower layer
     * @param layerNumber number of layer neuron belongs to
     * @param pEnableConstAddend indicates whether neuron should get additional constant input
     */
    Neuron(Network pNetwork, int numberOfInputs, int layerNumber, boolean pEnableConstAddend) {
        network = pNetwork;
        inputSum = 0.0D;
        output = 0.0D;
        delta = 0.0D;
        inputs = numberOfInputs;
        layer = layerNumber;
        enableConstAddend = pEnableConstAddend;
        weights = new double[numberOfInputs + 1];
        exWeights = new double[numberOfInputs + 1];
        changes = new double[numberOfInputs + 1];

        setNewWeights(false);
    }

    /**
     * Generates random weights for edges connecting this neuron with ones from previous layer
     *@param clearWeights tells whether all weights should be set to 0.0
     */
    void setNewWeights(boolean clearWeights) {
        Random randomEngine = new Random();
        if(clearWeights) {
            for(int i = 0; i < inputs + 1; ++i) {
                weights[i] = 0.0D;
                exWeights[i] = 0.0D;
                changes[i] = 0.0D;
            }
        }
        else {
            for(int i = 0; i <= inputs; ++i) {
                weights[i] = randomEngine.nextDouble() - 0.5;
                exWeights[i] = 0.0D;
                changes[i] = 0.0D;
            }
        }

    }

    /**
     *  activation function
     * @param signal sum of all signals sent from lower layer neurons
     * @return output signal
     */
    public abstract double activate(double signal);

    /**
     *
     * @param signal sum of all signals sent from lower layer neurons
     * @return the value of output's derivative
     */
    public abstract double activateDerivative(double signal);

    /**
     *  calculates output value based on values on neuron's input
     */
    public void calculateOutput() {
        inputSum = 0.0;
        //for output (linear) neuron
        if(layer == 0) {
            for(int i = 0; i < inputs; ++i) {
                inputSum += weights[i] * network.input[i];
            }
        }
        //for Sigmoidal neurons
        else {
            for(int i = 0; i < inputs; ++i) {
                inputSum += weights[i] * network.neuron[layer-1][i].output;
            }
        }
        if(enableConstAddend) {
            inputSum += weights[inputs];
        }
        output = activate(inputSum);
    }

    /**
     *  changes weights of edges due to backwards propagation
     * @param i neuron's position in its layer (considering layer as an array)
     */
    public void calculateCorrections(int i) {
        double tmp = 0.0;
        delta = 0.0;
        //Calculate the value of learning factor
        //a) if neuron belongs to the output layer
        if(layer == network.getNumberOfLayers()) {
            // simply calculating difference between output value and given correct value
            delta = (network.learnVector[i] - output);
        }
        //b) if neuron belongs to one of hidden layers
        else {
            // count number of neurons in previous layer
            int layersSize = network.getLayersSize(layer);
            // calculate sum of products of higher layer's neurons output values and the weights of connecting edges
            for(int j = 0; j < layersSize; ++j) {
                delta += network.neuron[layer+1][j].delta * network.neuron[layer+1][j].weights[i];
            }
        }
        // delta parameter equals sum calculated above times derivative of activation function
        delta *= activateDerivative(inputSum);
        // store value of formula in order to speed up calculations
        tmp = 2*network.learning_rate*delta;

        // Calculate new weights
        calculateNewWeights(tmp);

    }

    private void calculateNewWeights(double tmp) {
        //a) for output layer (linear) neuron
        if(layer == 0) {
            for(int j = 0; j < inputs; ++j) {
                changes[j] += tmp * network.input[j];
            }
        }
        //b) if neuron belongs to one of hidden layers
        else {
            for(int j = 0; j < inputs; ++j) {
                changes[j] += tmp * network.neuron[layer-1][j].output;
            }
        }

        // additional calculation if const input is enabled
        if (enableConstAddend)
            changes[inputs] += tmp;
    }

    /**
     * Changing values of neuron's weights using back propagation algorithm
     * @param i the number of neuron in its layer (considering layer as an array)
     * @param n number of iterations of calculating corrections
     */
    public void correctWeights(int i, int n) {
        double tmp;

        for(int j = 0; j < inputs; ++j) {
            //temporary store old value
            tmp = weights[j];
            // correct weight's value using MOMENTUM parameter
            weights[j] += changes[j] / (double)n +  MOMENTUM*(weights[j] - exWeights[j]);
            //store old value in class member
            exWeights[j] = tmp;
            // clear corrections value
            changes[j] = 0.0D;
        }
        // const input weight correction if const input is enabled
        if (enableConstAddend) {
            // temporary store old value
            tmp = weights[inputs];
            // change weight's value using MOMENTUM parameter
            weights[inputs] += changes[inputs] / (double)n +  MOMENTUM*(weights[inputs] - exWeights[inputs]);
            // store old value
            exWeights[inputs] = tmp;
            // clear value of calculated change
            changes[inputs] = 0.0;
        }
    }
}
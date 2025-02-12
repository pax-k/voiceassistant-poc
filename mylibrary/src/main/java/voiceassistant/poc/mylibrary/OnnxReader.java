package voiceassistant.poc.mylibrary;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtException;
import java.io.BufferedInputStream;
import ai.onnxruntime.OnnxTensor;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

public class OnnxReader {

    private OrtEnvironment env;
    private OrtSession session;

    public OnnxReader(Context context, String assetFileName) throws IOException, OrtException {
        byte[] modelBytes = loadModelFile(context, assetFileName);
        env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
        session = env.createSession(modelBytes, opts);
    }

    private byte[] loadModelFile(Context context, String assetFileName) throws IOException {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open(assetFileName);

        // Get the file size
        long fileSize = inputStream.available();
        if (fileSize > Runtime.getRuntime().maxMemory() / 4) {
            throw new IOException("Model file is too large to load into memory");
        }

        // Use BufferedInputStream for better performance
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        byte[] buffer = new byte[8192]; // 8KB buffer
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int bytesRead;
        while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        bufferedInputStream.close();
        inputStream.close();

        return outputStream.toByteArray();
    }

    public OrtSession getSession() {
        return session;
    }

    public void close() throws OrtException {
        if (session != null) {
            session.close();
        }
        if (env != null) {
            env.close();
        }
    }

    public String performInference(String inputText) throws OrtException {
        // Create input tensor
        OnnxTensor inputTensor = createInputTensor(inputText);

        // Run inference
        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input_ids", inputTensor);  // Replace "input" with your model's actual input name

        try {
            OrtSession.Result result = session.run(inputs);

            // Process the output tensor
            OnnxTensor outputTensor = (OnnxTensor) result.get(0);
            // Convert output tensor to string - adjust based on your model's output format
            float[] outputData = (float[]) outputTensor.getValue();

            return processOutput(outputData);
        } finally {
            inputTensor.close();
        }
    }
    private OnnxTensor createInputTensor(String inputText) throws OrtException {
        // Convert input text to the format your model expects
        // This is a placeholder - adjust based on your model's input requirements
        long[] inputData = new long[inputText.length()];
        for (int i = 0; i < inputText.length(); i++) {
            inputData[i] = (long) inputText.charAt(i);
        }

        long[] shape = new long[]{1, inputText.length()};  // Adjust shape based on your model
        return OnnxTensor.createTensor(env, LongBuffer.wrap(inputData), shape);
    }

    private String processOutput(float[] outputData) {
        // Process the output data based on your model's output format
        // This is a placeholder - implement based on your specific model
        StringBuilder result = new StringBuilder();
        for (float value : outputData) {
            result.append(value).append(" ");
        }
        return result.toString().trim();
    }
}

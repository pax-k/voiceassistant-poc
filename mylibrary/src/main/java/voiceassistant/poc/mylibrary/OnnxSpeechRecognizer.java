package voiceassistant.poc.mylibrary;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtException;

public class OnnxSpeechRecognizer {

    private OrtEnvironment env;
    private OrtSession session;

    public OnnxSpeechRecognizer(Context context, String assetFileName) throws IOException, OrtException {
        byte[] modelBytes = loadModelFile(context, assetFileName);
        initializeOrtSession(modelBytes);
    }

    private byte[] loadModelFile(Context context, String assetFileName) throws IOException {
        AssetManager assetManager = context.getAssets();
        try (InputStream is = assetManager.open(assetFileName);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }

    private void initializeOrtSession(byte[] modelBytes) throws OrtException {
        env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
        // Configure opts for NNAPI or other accelerators if needed

        if (android.os.Build.VERSION.SDK_INT >= 27) {
            opts.addNnapi();
        }

        // Set the number of CPU threads
        opts.setIntraOpNumThreads(2);
        session = env.createSession(modelBytes, opts);
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
}

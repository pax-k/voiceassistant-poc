package voiceassistant.poc.mylibrary;


import android.content.Context;

import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Tokenizer {
    private final Map<String, Long> vocab;
    private final Map<Long, String> reverseVocab;
    private final long PAD_TOKEN_ID = 0L;  // Assume 0 is the ID for padding token

    public Tokenizer(Context context) throws Exception {
        this.vocab = loadVocab(context, "tokenizer_vocab.json");
        this.reverseVocab = new HashMap<>();
        for (Map.Entry<String, Long> entry : vocab.entrySet()) {
            reverseVocab.put(entry.getValue(), entry.getKey());
        }
    }

    private Map<String, Long> loadVocab(Context context, String fileName) throws Exception {
        InputStream is = context.getAssets().open(fileName);
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        String json = new String(buffer, StandardCharsets.UTF_8);

        JSONObject jsonObj = new JSONObject(json);
        Map<String, Long> vocabMap = new HashMap<>();

        Iterator<String> keys = jsonObj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Long value = jsonObj.getLong(key);  // Get the value associated with the key
            vocabMap.put(key, value);
        }
        return vocabMap;
    }
    private Map<String, Long> parseVocabJson(String json) {
        // Assume JSON is in format {"word": id, "another": id, ...}
        Map<String, Long> vocab = new HashMap<>();
        json = json.replaceAll("[{}\"]", "").trim();
        for (String entry : json.split(",")) {
            String[] kv = entry.split(":");
            vocab.put(kv[0].trim(), Long.parseLong(kv[1].trim()));
        }
        return vocab;
    }

    public TokenizedOutput tokenize(String text) {
        String[] words = text.split(" ");
        long[] tokenIds = new long[words.length];
        long[] attentionMask = new long[words.length];

        int maxLength =10;

        // Tokenize and generate attention mask
        for (int i = 0; i < words.length; i++) {
            tokenIds[i] = vocab.getOrDefault(words[i], PAD_TOKEN_ID); // 0 for unknown tokens
            attentionMask[i] = (tokenIds[i] == PAD_TOKEN_ID) ? 0L : 1L;  // 1 for valid tokens, 0 for padding/unknown
        }

        // Pad if necessary
        int paddingLength = maxLength - tokenIds.length;
        if (paddingLength > 0) {
            long[] paddedTokenIds = new long[maxLength];
            long[] paddedAttentionMask = new long[maxLength];

            System.arraycopy(tokenIds, 0, paddedTokenIds, 0, tokenIds.length);
            System.arraycopy(attentionMask, 0, paddedAttentionMask, 0, attentionMask.length);

            // Add padding
            for (int i = tokenIds.length; i < maxLength; i++) {
                paddedTokenIds[i] = PAD_TOKEN_ID;
                paddedAttentionMask[i] = 0L;
            }

            return new TokenizedOutput(paddedTokenIds, paddedAttentionMask);
        }

        return new TokenizedOutput(tokenIds, attentionMask);
    }


    public static class TokenizedOutput {
        public long[] tokenIds;
        public long[] attentionMask;

        public TokenizedOutput(long[] tokenIds, long[] attentionMask) {
            this.tokenIds = tokenIds;
            this.attentionMask = attentionMask;
        }

    public long[] getInputIds() {
        return tokenIds;
    }

    public long[] getAttentionMask() {
        return attentionMask;
    }
    }
    public String detokenize(long[] tokenIds) {
        StringBuilder result = new StringBuilder();
        for (long id : tokenIds) {
            result.append(reverseVocab.getOrDefault(id, "[UNK]")).append(" ");
        }
        return result.toString().trim();
    }
}

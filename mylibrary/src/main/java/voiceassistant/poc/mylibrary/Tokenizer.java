package voiceassistant.poc.mylibrary;


import android.content.Context;

import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
        // Preprocess text: add space prefix for GPT-style tokenization
        text = " " + text.trim();
        
        // Split into potential tokens, preserving spaces
        StringBuilder currentToken = new StringBuilder();
        java.util.List<String> tokens = new ArrayList<>();
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken = new StringBuilder();
                }
                // Add space prefix token
                tokens.add("Ġ" + String.valueOf(text.charAt(Math.min(i + 1, text.length() - 1))));
                i++; // Skip next character as it's included in the space token
            } else {
                currentToken.append(c);
            }
        }
        // Add last token if exists
        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
        }

        // Convert tokens to IDs with fixed length
        int maxLength = 512; // or your model's maximum sequence length
        long[] tokenIds = new long[maxLength];
        long[] attentionMask = new long[maxLength];
        
        // Fill token IDs and attention mask
        for (int i = 0; i < maxLength; i++) {
            if (i < tokens.size()) {
                String token = tokens.get(i);
                // Try different token variations
                Long id = vocab.get(token);
                if (id == null) {
                    id = vocab.get("Ġ" + token);
                }
                if (id == null) {
                    id = vocab.get(token.toLowerCase());
                }
                tokenIds[i] = id != null ? id : PAD_TOKEN_ID;
                attentionMask[i] = id != null ? 1L : 0L;
            } else {
                tokenIds[i] = PAD_TOKEN_ID;
                attentionMask[i] = 0L;
            }
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
            String token = reverseVocab.getOrDefault(id, "");
            if (!token.isEmpty()) {
                // Handle special tokens
                if (token.startsWith("Ġ")) {
                    // Remove the special prefix and add space
                    result.append(" ").append(token.substring(1));
                } else {
                    result.append(token);
                }
            }
        }
        // Clean up the text
        return result.toString()
                .trim()
                .replaceAll("\\s+", " ")
                .replaceAll("Ġ", ""); // Remove any remaining special tokens
    }
}

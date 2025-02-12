# Project Setup Guide

This guide explains the steps to set up a project with a basic views activity and integrate ONNX for machine learning inference.

---

## Steps

### Step 1: Set Up a Basic Views Activity
- **Task**: Initialize a new project.
- **Action**: Create a basic activity to serve as the main UI.

---

### Step 2: Create a Library Module
- **Task**: Add a new library module.
- **Action**: Name the module `mylibrary` to allow for modularization and reusability.

---

### Step 3: Add Required Permission
- **File**: `AndroidManifest.xml` (in the app module).
- **Action**: Add the following permission for audio recording:

  ```xml
  <uses-permission android:name="android.permission.RECORD_AUDIO" />


### Step 4: Add ONNX Dependency
- **File**: `build.gradle` (app module).
- **Task**: Integrate the ONNX Runtime library to enable inference using ONNX models.
- **Action**: Add the following dependency to the `dependencies` block:

  ```gradle
  implementation("com.microsoft.onnxruntime:onnxruntime-android:latest.release")

### Step 5: Download the ONNX Model
- **Source**: Download the `model.onnx` file from [HuggingFace](https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct/tree/main/onnx).
- **Task**: Place the model file in the `assets` folder of the app module.
- **Action**:
    1. Locate or create the `assets` directory under `src/main`.
    2. Copy the `model.onnx` file into this directory.

---

### Step 6: Create `OnnxReader` Class
- **Location**: `mylibrary` module.
- **Task**: Implement a utility class for handling ONNX model operations.
- **Action**:
    1. Create a new Java/Kotlin class named `OnnxReader`.
    2. Write methods to:
        - Load the `model.onnx` file.
        - Execute inference using the ONNX Runtime library.

---

### Step 7: Implement Inference in `MyActivity`
- **Location**: `MyActivity` in the app module.
- **Task**: Add logic for performing inference using the ONNX model.
- **Action**:
    1. Use the `OnnxReader` class to load the model.
    2. Process input data through the model.
    3. Display or utilize the output as needed.

### Step 8: Implement the Tokenizer Class Using JSON File
- **Location**: `mylibrary` module.
- **Task**: Create a tokenizer class that processes input text into `input_ids` and `attention_mask`.

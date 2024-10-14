### Offline Mobile Chatbot for Device Health Optimization

#### Project Summary

This project involves creating a mobile application that allows users to chat with their smartphone about its health. The app provides personalized recommendations on optimizing device performance, with the key requirement being that it operates entirely offline.

#### Solution Overview

The proposed solutions considered include:

- ❌ **Rule-Based System**: This approach was deemed **unsuitable** due to its **limitations** in handling the extensive variety of potential user inquiries.

- ✅ **LLM-Based System (Large Language Model)**:

  - Provides **extensive knowledge:** can handle wiede variety of user inquries.
  - **Limitations**: On-device use is constrained due to large model sizes and resource demands.

To proceed, the chosen methods to create an offline LLM include:

- **❌ KerasNLP for NLP Tasks**: To provide basic natural language understanding capabilities, recognizing that existing tools like **KerasNLP** only offer completion with **GPT-2** and lack sufficient instruction-following models.

- **❌ ONNX Conversion**: Model conversion to ONNX format for on-device efficiency. Microsoft's Phi-3.5-mini-instruct was tested but found to be too large (>2.5GB) and slow for efficient on-device inference.

- **✅ Google MediaPipe**: MediaPipe Solutions offers tools and libraries for applying AI and ML techniques in applications, with options for customization. It was tested and showed acceptable results. Additionally, it includes Gemma1.1-2b-instruct with int4 quantization, making it fast in inference.

#### The next step involves finding a method to make the scope of the chosen LLM limited to smartphone-related questions. The proposed solutions were:

- **❌Fine-tuning:** The absence of a pre-existing dataset and time constraints made fine-tuning an LLM challenging.
- **❌Prompt Engineering:** Small LLMs struggle to follow instructions accurately and are unreliable without sufficient fine-tuning. Additionally, some models (including Gemma1.1-2b) were not trained with system instructions, making it hard to define the LLM scope to be about smartphones only. 
- **✅Rule-Based approach:** the implmented approach was to only accept user input that contain certian keywords related to smartphones (eg. ram, battery, os, etc.) \
  [https://ai.google.dev/edge/mediapipe/solutions/genai/llm\_inference/android](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android)

To differentiate between general recommendations and specific details about the user's own device, a keyword-based approach is used. Specific keywords "sysinfo" help determine the nature of the user's inquiry. 

Based on the keyword identification, the chatbot provides insights on device health metrics like RAM usage, storage, and battery, alongside relevant recommendations for optimization.

#### Key Considerations

- **Distinguishing User Intent**: Implemented logic to distinguish between general questions and device-specific inquiries using prompt keywords.
- **Offline Functionality**: All responses generated are based on an **on-device** LLM to ensure privacy and availability without network access.

#### Bonus Feature

An online LLM (Qwen72B-instruct) model is used for online inference, integrated via the Hugging Face API. This was achieved through prompt engineering and the model's substantial capabilities (72 billion parameters). This allows it to generate more rigorous and contextually accurate responses. Additionally, the model automatically detects when the user is asking about their specific device, eliminating the need for predefined keywords (like "sysinfo") by using context to determine the user's intent, while maintaining a natural flow in conversation.


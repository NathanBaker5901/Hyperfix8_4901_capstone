# F.A.Q

### Q: Why did you decide to do this project?
A: We decided to pursue this project because we saw that having the opportunity to experience mobile app development and being able to learn the technical side of object detection was very valuable.

### Q: Why did you choose to create the project in android studio?
A: We didn't at first, we started in python and quickly realized that the libraries and imports were less than ideal for what we had planned. Android studio had a lot more tools and imports for us to use and since we used Kotlin as our primary language, making a mobile interface became rather easy.

### Q: Why did you choose Kotlin and what issues did you face using it?
A: We decided to use Kotlin because most apps recommend using java or some form of java as their coding language. When we discovered that Kotlin is the “easier java” we decided to pursue it as our coding language. It was easy to learn, easy to use, the imports were easy to add and the range of imports was massive. We didn't face that many challenges with kotlin once we got going, so the big challenge was just learning a new language. 

### Q: How did you figure out how to detect legos in your application?
A: We trained a custom ai model in MLK kit using tensorflow. Which was a software that allowed users to be able to detect objects in either the camera or in the gallery. We started small with one to two different legos/technic pieces and hope to add more since training a model takes a lot of time and resources.

### Q: Can you explain what the app does?
A: Block Lens is a mobile app that uses a custom trained AI model that identifies various Legos via the camera. It also has some accessibility features that will help assist the visually impaired navigate and use the application.

### Q: What kinds of trouble did you encounter when developing the project?
A: There were two main issues we faced while developing this app. We started off in python, so everything became difficult right from the start, especially the camera features. OpenCV was complex and difficult to get integrated with, in addition it would take almost an hour to get a running version on our app since we weren't able to run a phone emulator. So every time we wanted to test something, we'd have to essentially deploy our app which is inefficient and time consuming. Then once we swapped over to android studio and kotlin, training an AI model became the bane of our existence since that was new and none of us had experience doing that before.

### Q: What experiences did you gain from this project?
A: The biggest experience for some of us training that AI model. That takes hours and hours of patience waiting on that to finish training and then translating that model into the correct format so that tensor flow or other resources could be used. But other than that, the biggest experience was learning how to collaborate and communicate effectively. We needed to make sure we are all on the same page day-in and day-out as well as maintain focus on our objective for that week.

### Q: If your team was behind on a deadline, what was the team mindset and how did you solve the issue?
A: We primarily went to a goal oriented mindset. All that means is we sat down, and really thought about the little wins we could achieve in order to help us reach our deadline. Worst case scenario, we were upfront to our sponsor and held ourselves accountable about falling behind.

### Q: Do you have any aspiration to further develop this project after your tenure at UNT?
A: This project was created to help assist our graduate student complete her research as more of a proof of concept. We laid the groundwork to help expand the ai model but as for us, if they ask for our assistance again then we'd be happy too.

### Q: How does the dynamic text size work?
A: When defining each text section, we had to apply a font family, font size, and the text displayed. When defining the font size, we applied a function that would check to see which “state” (which button) was selected and pull the corresponding font size. This was applied to all the text across the app. 

### Q: How does the colorblind mode work?
A: We created a colorbank within our app that defined each color and its respective corrected color translation depending on the colorblind mode selected. We also went with the three most commonly found visual impairments that most video games would have.

### Q: Why did you choose this type of UI?
A: We went for a simple, easy, light ui with very little functionality. We prioritized labeling each button and function so it was easy to interpret, and allowed for users to customize their experience by adjusting the available accessibility functions. We also chose blue as our base color since it would be easy on the eyes and less adjusting to various colorblind modes.

### Q: How does the Voice readback work?
A: The voice readback uses the existing android text-to-speech engine and real-time audio output. It reads our labels on our objects and the objects become descriptors so the user knows what they are pressing. 

### Q: How does object detection work?
A: We used MLKit to help assist with object detection. After training the an ai model, MLKit will take that trained model and run its algorithm and then identify the object accordingly. 

### Q: How does someone train an ai model?
A: In order for someone to train an ai model the person will need a data set. But for a single object we would need nearly 100 pictures of a single object at various angels, rotations, and lighting. With these photos, we need to use software that can help annotate and create a bounding box around the object in each photo, and these annotations are put into a format that can be used in various training models. From there, we feed these annotations into a training package run in python, these can vary as well as the packages and dependencies, from there it spits out a trained model file that we then need to translate into an interpreter. This is where TensorFlowLite comes into play, it reads the file contents and helps the object detection identify what it was trained on. After you do all of the steps then youll have a finished ai model in which it is trained to recognize a set of objects that you provided information through the data sets.

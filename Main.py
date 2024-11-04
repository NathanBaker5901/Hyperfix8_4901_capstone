from kivy.config import Config
Config.set('kivy', 'camera', 'opencv')

from kivy.app import App 
from kivy.uix.button import Button #for the buttons
from kivy.uix.boxlayout import BoxLayout # for the layout (there are different types of layouts in kivy this is just the most basic one)
from kivy.uix.floatlayout import FloatLayout # for non dynamic layout so the buttons dont move for camera page
from kivy.uix.screenmanager import ScreenManager, Screen #for the screen manager to track which screen is being shown 
from kivy.uix.label import Label # for the label like headers 
from kivy.uix.widget import Widget # adds widget for each of the classes
from kivy.graphics import Color, Ellipse # adds color to the circle
from kivy.core.window import Window # Sets the background color for app
from kivy.uix.camera import Camera  #Kivy's built-in Camera widget
from kivy.uix.popup import Popup #For dialog window for popups when needed
from kivy.uix.image import Image #Widgets for displaying selected images
from kivy.uix.label import Label #Text label widget
from plyer import filechooser #From plyer library for image selection
import os #Python module for operating system interactions

#class for the Front page child function of the ScreenManager class imported above
class FrontPage(Screen, Widget):
    #constructor for the front page screen
    pass

#class for the settings page
class SettingsPage(Screen, Widget):
    pass
    
#class for the camera page
class CameraPage(Screen, Widget):
    pass
# Class for the camera page
class CameraPage(Screen):
    def __init__(self, **kwargs):
        super(CameraPage, self).__init__(**kwargs)
        self.camera = None # For camera
        self.image_cache = None  # Cache to store selected image
        self.analyze_button = None  # Analyze button

    # Start camera when entering the camera page (DO NOT want the camera on at all times)
    def on_enter(self, *args):        
        if self.camera is None:
            self.camera = Camera(play=True, resolution=(640, 480), size_hint=(1, 1), pos_hint={'x': 0, 'y': 0})
            self.add_widget(self.camera)  

    # Stops camera when leaving camera page (stops the camera to free resources and makes sure its closed down when using app)
    def on_leave(self, *args):
        if self.camera is not None:
            self.camera.play = False
            self.remove_widget(self.camera)
            self.camera = None
    
    # Function that opens gallery (Works on windows should translate well to android according to kivy.org)
    def open_gallery(self, *args):
        # Opens the file chooser to select images
        filechooser.open_file(on_selection=self.display_image)
 
    # Display the selected image in a popup
    def display_image(self, selection):
        if selection:
            # Cache for the selected image
            self.image_cache = selection[0] 

            # Create a popup for the image
            popup_content = FloatLayout()

            # Image widget to display selected image
            img = Image(source=selection[0], allow_stretch=True, size_hint=(1, 0.8), pos_hint={'x': 0, 'y': 0.2})
            popup_content.add_widget(img)

            # Close button on top right for the popup
            close_button = Button(text="X", size_hint=(None, None), size=(40, 40), pos_hint={'right': 0.98, 'top': 0.98})
            close_button.bind(on_press=lambda *x: popup.dismiss())
            popup_content.add_widget(close_button)

            # Analyze button within selected image
            self.analyze_button = Button(text="Analyze", size_hint=(None, None), size=(100, 50), pos_hint={'center_x': 0.5, 'y': 0.05})
            self.analyze_button.bind(on_press=self.cache_image)
            popup_content.add_widget(self.analyze_button)

            # Create and open the popup
            popup = Popup(title='Selected Image', content=popup_content, size_hint=(0.8, 0.8))
            popup.open()

    # Cache the selected image (This will save items in cache in theory we will use the cache as the location to send the image to the machine learning function)
    def cache_image(self, instance):
        # Checks if cache location exists
        if self.image_cache: # Cache location does exist
            cache_dir = './image_cache' # Saves image in cache
            if not os.path.exists(cache_dir): # Cache location does not exist
                os.mkdir(cache_dir) # Creates cache location if not

            # Selects a location/path for the cached image 
            cached_image_path = os.path.join(cache_dir, 'cached_image.png')
            with open(self.image_cache, 'rb') as source_file:
                with open(cached_image_path, 'wb') as dest_file:
                    dest_file.write(source_file.read())

            print(f"Image cached at: {cached_image_path}") # Output letting us know the image is in the cache location

#class for the manual page
class ManualPage(Screen, Widget):
    pass

#main function for the app
class MainApp(App, Widget):

    #function for the root of the of the widgets where the apps UI will be added 
    def build(self):
        # Sets the background color blue based off the wireframe
        Window.clearcolor = (0/255, 82/255, 163/255, 1)
        sm = ScreenManager()

        #add the pages to the screen manager    
        sm.add_widget(FrontPage(name='front_page'))
        sm.add_widget(SettingsPage(name='settings_page'))
        sm.add_widget(CameraPage(name='camera_page'))
        sm.add_widget(ManualPage(name='manual_page'))

        #return the screen manager
        return sm

#run the app
if __name__ == '__main__':
    MainApp().run()


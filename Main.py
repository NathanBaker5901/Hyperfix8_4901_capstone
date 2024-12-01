import cv2
#from kivy.config import Config
#Config.set('kivy', 'camera', 'opencv')


from kivy.app import App 
from kivy.lang import Builder #building files correctly
from kivy.uix.button import Button #for the buttons
from kivy.lang import Builder
from kivy.uix.boxlayout import BoxLayout # for the layout (there are different types of layouts in kivy this is just the most basic one)
from kivy.uix.floatlayout import FloatLayout # for non dynamic layout so the buttons dont move for camera page
from kivy.uix.screenmanager import ScreenManager, Screen #for the screen manager to track which screen is being shown 
from kivy.uix.label import Label # for the label like headers 
from kivy.uix.widget import Widget # adds widget for each of the classes
from kivy.uix.dropdown import DropDown # adds dropdown widgets
from kivy.graphics import Color, Ellipse, Rectangle # adds color to the circle
from kivy.graphics.texture import Texture #used for OpenCV image data
from kivy.core.window import Window # Sets the background color for app
from kivy.uix.camera import Camera  #Kivy's built-in Camera widget
from kivy.uix.popup import Popup #For dialog window for popups when needed
from kivy.uix.image import Image #Widgets for displaying selected images
from kivy.properties import NumericProperty, StringProperty # control text values
from kivy.utils import get_color_from_hex # color translating from hex to rgba
from plyer import filechooser #From plyer library for image selection
from PIL import Image as PILImage #Python Image Library
from PIL import ImageDraw, ImageFont #Python Image Library
from kivy.clock import Clock #Used to capture frames form OpenCV Camera
import os #Python module for operating system interactions
import numpy as np #Used for handling image data from OpenCV's numpy arrays

#SETTING WINDOW SIZE FOR NOW YOU CAN COMMENT THIS OUT TO TURN IT OFF
#Window.size = (412, 900) #Aspect Ratio 20:9 (reflects Google Pixel 9 1080x2424)
#Window.resizable = False   #Locking resize window for desktop purposes

#class for the Front page child function of the ScreenManager class imported above
class FrontPage(Screen, Widget):
    #constructor for the front page screen
    pass

#class for the settings page 
class SettingsPage(Screen, Widget):
    
    #ABOUT US PAGE TEXT
    about_us_text = StringProperty(
        """Version: Beta 1.0.0

Features: List of features here

Authors: Nathan Baker, Carlos Garcia, Joel Hunt, Abel Montoya, Andres Montoya

Contributions: Any additional contributions such as icon usage or images used

Our Mission: Fill in with summary of our mission and apps purpose"""
    )

    pass

#Builder.load_file('Main.kv')
# Class for the Image popup
class ImagePopup(Popup):
    pass
# Class for the camera page

class CameraPage(Screen):
    def __init__(self, **kwargs):
        super(CameraPage, self).__init__(**kwargs)
    
        self.cap = None  # OpenCV VideoCapture
        self.image_cache = None
        self.frame_event = None
        self.current_frame = None # Captures the current frame

    # Start OpenCV camera when user is in the camera page
    def on_enter(self, *args):
        self.start_opencv_camera()

    # Stop OpenCV camera when user exits the camera page
    def on_leave(self, *args):
        if self.cap:
            self.cap.release()  # Release the camera
            cv2.destroyAllWindows()
            print("Camera has been released.")

        if self.frame_event:
            self.frame_event.cancel()  # Cancel frame update

    # Start OpenCV camera
    def start_opencv_camera(self):
        self.cap = cv2.VideoCapture(0)  # Useing camera index 0 for default camera
        if not self.cap.isOpened():
            print("Error: Could not open OpenCV camera")
        else:
            print("OpenCV camera started.")
            # Schedule the update of camera frames by 30 FPS
            self.frame_event = Clock.schedule_interval(self.update_camera_feed, 1.0 / 30.0)

    # Update the Image widget with the camera feed
    def update_camera_feed(self, dt):
        if self.cap:
            ret, frame = self.cap.read()
            if ret:
                self.current_frame = frame  # Store the current frame
                # Convert BGR frame to RGB
                buf = cv2.flip(frame, 0).tostring()
                texture = Texture.create(size=(frame.shape[1], frame.shape[0]), colorfmt='bgr')
                texture.blit_buffer(buf, colorfmt='bgr', bufferfmt='ubyte')
                self.ids.camera_feed.texture = texture

    # Capture image using the current frame from OpenCV
    def capture_opencv_image(self):
        if self.current_frame is not None:
            # Convert OpenCV frame (BGR) to PIL Image (RGB)
            pil_image = PILImage.fromarray(cv2.cvtColor(self.current_frame, cv2.COLOR_BGR2RGB))

            # Save to cache directory
            cache_dir = './image_cache'
            if not os.path.exists(cache_dir):
                os.mkdir(cache_dir)
            cached_image_path = os.path.join(cache_dir, 'captured_image.png')
            pil_image.save(cached_image_path)
            self.image_cache = cached_image_path

            print(f"Image captured and cached at: {cached_image_path}")
        else:
            print("Error: No frame available to capture")

    # Function that opens gallery works for windows and should work for android
    def open_gallery(self, *args):
        # Opens the file chooser to select images
        filechooser.open_file(on_selection=self.display_image)

    # Display the selected image in a popup
    def display_image(self, selection):
        if selection:
            # Cache for the selected image
            self.image_cache = selection[0]
            # Create/open popup
            image_popup = ImagePopup()
            image_popup.ids.img.source = self.image_cache
            image_popup.open()

    # Cache the selected image
    def cache_image(self, instance):
        # Checks if cache location exists
        if self.image_cache:
            cache_dir = './image_cache'  # Cache directory
            if not os.path.exists(cache_dir):
                os.mkdir(cache_dir)  # Create cache location if does not existing

            # Select a location/path for the cached image
            cached_image_path = os.path.join(cache_dir, 'cached_image.png')
            try:
                with open(self.image_cache, 'rb') as source_file:
                    with open(cached_image_path, 'wb') as dest_file:
                        dest_file.write(source_file.read())

                print(f"Image cached at: {cached_image_path}")
            except Exception as e:
                print(f"Error caching the image: {e}")
        else:
            print("Error: No image selected for caching")

#class for the manual page
class ManualPage(Screen, Widget):
    pass

#main function for the app
class MainApp(App, Widget):
    # text sizes, labels and regular font
    # Settings page will update sizes
    text_size_labels = NumericProperty(24)
    text_size_default_font = NumericProperty(14)
    current_scheme = "Default" #setting color scheme
    current_font_size = "Default" #track current selected font size

    #Function for font size
    def set_font_size(self, size):
        if size == 'Small':
            self.text_size_default_font = 10
            self.text_size_labels = 14
            self.current_font_size = size  # Update the current font size
            self.update_settings_page_colors(self.colors[self.current_scheme])  # Refresh button colors
        elif size == 'Default':
            self.text_size_default_font = 14
            self.text_size_labels = 24
            self.current_font_size = size  # Update the current font size
            self.update_settings_page_colors(self.colors[self.current_scheme])  # Refresh button colors
        elif size == 'Large':
            self.text_size_default_font = 20
            self.text_size_labels = 28
            self.current_font_size = size  # Update the current font size
            self.update_settings_page_colors(self.colors[self.current_scheme])  # Refresh button colors

    #COLOR DEFINITIONS FOR COLORBLIND MODE
    #USING get_color_from_hex (currently testing)
    colors = {
        "Default": {
            "Main Color": get_color_from_hex("#0052A3"),
            "Selected Box Color": get_color_from_hex("#0051a3"),
            "Non-Selected Color": get_color_from_hex("#002447"),
            "Background Color": get_color_from_hex("#00407e"),
            "Highlight Box Color": get_color_from_hex("#ff0000"),
            "Text Color": get_color_from_hex("#FFFFFF"),
            "Border Color": get_color_from_hex("#000000"),
        },
        "Protanopia": {
            "Main Color": get_color_from_hex("#2451A0"),
            "Selected Box Color": get_color_from_hex("#2451A0"),
            "Non-Selected Color": get_color_from_hex("#2151A0"),
            "Background Color": get_color_from_hex("#182747"),
            "Highlight Box Color": get_color_from_hex("#8F7E1E"),
            "Text Color": get_color_from_hex("#FFFFFF"),
            "Border Color": get_color_from_hex("#000000"),
        },
        "Deuteranopia": {
            "Main Color": get_color_from_hex("#005693"),
            "Selected Box Color": get_color_from_hex("#005592"),
            "Non-Selected Color": get_color_from_hex("#002947"),
            "Background Color": get_color_from_hex("#004475"),
            "Highlight Box Color": get_color_from_hex("#A17800"),
            "Text Color": get_color_from_hex("#FFFFFF"),
            "Border Color": get_color_from_hex("#000000"),
        },
        "Tritanopia": {
            "Main Color": get_color_from_hex("#005D63"),
            "Selected Box Color": get_color_from_hex("#005C62"),
            "Non-Selected Color": get_color_from_hex("#002D2F"),
            "Background Color": get_color_from_hex("#004A4E"),
            "Highlight Box Color": get_color_from_hex("#FD1700"),
            "Text Color": get_color_from_hex("#FFFFFF"),
            "Border Color": get_color_from_hex("#000000"),
        }
    }

    #Function for color schemeing pages
    def apply_color_scheme(self, mode):
        self.current_scheme = mode

        # Sets the main color blue based off the wireframe
        Window.clearcolor = self.get_color("Main Color")

    #function for the root of the of the widgets where the apps UI will be added 
    def build(self):
        
        sm = ScreenManager()

        #add the pages to the screen manager    
        sm.add_widget(FrontPage(name='front_page'))
        sm.add_widget(SettingsPage(name='settings_page'))
        sm.add_widget(CameraPage(name='camera_page'))
        sm.add_widget(ManualPage(name='manual_page'))

        self.apply_color_scheme("Default") #Setting color scheme as default

        #return the screen manager
        return sm
    
    # Set up the dropdown menu for color blind
    def on_start(self):
            
            #setting selected button for text size
            self.update_settings_page_colors(self.colors[self.current_scheme])

            #setting color blind button and selection
            settings_screen = self.root.get_screen('settings_page')
            color_blind_button = settings_screen.ids.color_blind_button

            # Creating Dropdown and adding options
            dropdown = DropDown()
            color_blind_button.bind(on_release=lambda btn: self.open_dropdown(dropdown, btn))

    def open_dropdown(self, dropdown, button):
        # Clear the dropdown each time before opening
        dropdown.clear_widgets()

        # Define all possible options
        options = ["Default", "Protanopia", "Deuteranopia", "Tritanopia"]
        
        # Standardize the button text to match keys in the dictionary
        current_scheme = button.text.strip().capitalize()  # Standardize capitalization

        # Add only options that are not the current selection
        for option in options:
            if option != button.text:
                btn = Button(text=option, size_hint_y=None, height='40dp', font_size=self.text_size_default_font)
                btn.background_color = self.colors[current_scheme].get("Non-Selected Color")
                btn.color = self.colors[current_scheme].get("Text Color")

                #Bind selection action
                btn.bind(on_release=lambda btn: self.select_dropdown_option(dropdown, button, btn.text))
                dropdown.add_widget(btn)

        # Open the dropdown
        dropdown.open(button)

    def select_dropdown_option(self, dropdown, button, selected_option):
        # Set the button text to the selected option
        button.text = selected_option

        # Dismiss the dropdown
        dropdown.dismiss()

        # Apply the new color scheme
        self.apply_color_scheme(selected_option)
        self.update_settings_page_colors(self.colors[selected_option])

    #Function to pull current color scheme and colors
    def get_color(self, color_name):
        return self.colors[self.current_scheme].get(color_name)
    
    #Function to update settings page color elements
    #will probably need to update this name and add all elements across app
    def update_settings_page_colors(self, color_scheme):

        #Control for updating elements of ui (SETTINGS ONLY RN)
        settings_screen = self.root.get_screen('settings_page')

        #Setting Page element updates
        settings_screen.ids.back_button.background_color = color_scheme["Text Color"]
        settings_screen.ids.back_button.color = color_scheme["Border Color"]
        settings_screen.ids.settings_label.color = color_scheme["Text Color"]
        settings_screen.ids.color_blind_button.background_color = color_scheme["Non-Selected Color"]
        settings_screen.ids.color_blind_button.color = color_scheme["Text Color"]

        #Update Text Size Selected/Non-selected buttons
        for size, btn_id in zip(['Small', 'Default', 'Large'], ["text_size_small", "text_size_default", "text_size_large"]):
            button = settings_screen.ids.get(btn_id)
            if button:
                button.background_color = color_scheme["Selected Box Color"] if size == self.current_font_size else color_scheme["Non-Selected Color"]
                button.color = color_scheme["Text Color"]

#run the app
if __name__ == '__main__':
    MainApp().run()
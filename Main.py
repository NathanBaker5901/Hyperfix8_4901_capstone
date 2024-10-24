from kivy.config import Config
Config.set('kivy', 'camera', 'opencv')

from kivy.app import App 
from kivy.uix.button import Button #for the buttons
from kivy.uix.boxlayout import BoxLayout # for the layout (there are different types of layouts in kivy this is just the most basic one)
from kivy.uix.floatlayout import FloatLayout # for non dynamic layout so the buttons dont move for camera page
from kivy.uix.screenmanager import ScreenManager, Screen #for the screen manager to track which screen is being shown 
from kivy.uix.label import Label # for the label like headers 
from kivy.uix.camera import Camera  #Kivy's built-in Camera widget


#class for the Front page child function of the ScreenManager class imported above
class FrontPage(Screen):
    #constructor for the front page screen
    pass

#class for the settings page
class SettingsPage(Screen):
    pass
    
#class for the camera page
class CameraPage(Screen):
    #constructor for the camera page
    def __init__(self, **kwargs):
        super(CameraPage, self).__init__(**kwargs)

        # Use FloatLayout to position widgets absolutely
        self.main_layout = FloatLayout()

        # Create the placeholder for the camera
        self.camera = None

        # Horizontal layout for buttons (Go Home and Gallery)
        self.button_layout = BoxLayout(orientation='horizontal', size_hint_y=0.125)

        # Button to go back to the front page
        button_home = Button(text='Go to Home Page')
        button_home.bind(on_press=self.go_to_home_page)
        self.button_layout.add_widget(button_home)   

        # Placeholder button for gallery
        button_gallery = Button(text='Gallery')
        # No functionality for gallery button as of now
        self.button_layout.add_widget(button_gallery)

        # Add the button layout to the main layout
        self.main_layout.add_widget(self.button_layout)

        # Add the main layout to the screen
        self.add_widget(self.main_layout)
    
    # Start camera when entering the camera page
    def on_enter(self, *args):
        if self.camera is None:
            # Initialize the Camera widget when the user enters the camera page
            self.camera = Camera(play=True, resolution=(640, 480), size_hint=(1, 0.9), pos_hint={'x': 0, 'y': 0.1})
            self.main_layout.add_widget(self.camera)  # Add the camera

    # Stops camera when leaving page
    def on_leave(self, *args):
        if self.camera is not None:
            self.camera.play = False  # Stop the camera feed
            self.main_layout.remove_widget(self.camera)  # Remove the camera widget
            self.camera = None  # Free up resources
            # Explicitly attempt to release resources
            try:
                self.camera._camera._release_camera()  # For Kivy's internal camera class
            except AttributeError:
                print("Explicit camera release not supported by backend")
            print("Camera stopped and removed")

    # Method to go back to the front page
    def go_to_home_page(self, instance):
        self.manager.current = 'front_page'
        self.on_leave()  # Call on_leave manually to ensure the camera is stopped when navigating away
 

#class for the manual page
class ManualPage(Screen):
    pass

#main function for the app
class MainApp(App):

    #function for the root of the of the widgets where the apps UI will be added 
    def build(self):
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


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

    # Start camera when entering the camera page
    def on_enter(self, *args):
        self.camera = self.ids.camera
        if not self.camera.play:
            self.camera.play = True
    # Stops camera when leaving page
    def on_leave(self, *args):
        if self.camera and self.camera.play:
            self.camera.play = False

 

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


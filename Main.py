from kivy.app import App 
from kivy.uix.button import Button #for the buttons
from kivy.uix.boxlayout import BoxLayout # for the layout (there are different types of layouts in kivy this is just the most basic one)
from kivy.uix.screenmanager import ScreenManager, Screen #for the screen manager to track which screen is being shown 
from kivy.uix.label import Label # for the label like headers 
from kivy.uix.widget import Widget # adds widget for each of the classes
from kivy.graphics import Color, Ellipse # adds color to the circle
from kivy.core.window import Window # Sets the background color for app
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

    #constructor for the camera page   

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


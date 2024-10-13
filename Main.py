from kivy.app import App 
from kivy.uix.button import Button #for the buttons
from kivy.uix.boxlayout import BoxLayout # for the layout (there are different types of layouts in kivy this is just the most basic one)
from kivy.uix.screenmanager import ScreenManager, Screen #for the screen manager to track which screen is being shown 
from kivy.uix.label import Label # for the label like headers 

#class for the Front page child function of the ScreenManager class imported above
class FrontPage(Screen):

    #constructor for the front page screen
    def __init__(self, **kwargs): 
        super(FrontPage, self).__init__(**kwargs)
        layout = BoxLayout(orientation='vertical')
        
        #add a label to the screen
        self.label = Label(text = "Welcome to the Title Page", font_size = 20)
        layout.add_widget(self.label)

        #make a buttons to go to the setting page, camera page, and the manual page
        button_settings = Button(text = 'go to settings page')
        button_settings.bind(on_press = self.go_to_settings_page)
        layout.add_widget(button_settings)

        button_camera = Button(text = 'go to camera page')
        button_camera.bind(on_press = self.go_to_camera_page)
        layout.add_widget(button_camera)

        button_manual = Button(text = 'go to manual page')
        button_manual.bind(on_press = self.go_to_manual_page)
        layout.add_widget(button_manual)

        #add the layout to the screen 
        self.add_widget(layout)
    
    #child functions for the buttons to go to the settings page, camera page, and the manual page
    def go_to_settings_page(self, instance):
        self.manager.current = 'settings_page'
    
    def go_to_camera_page(self, instance):
        self.manager.current = 'camera_page'

    def go_to_manual_page(self, instance):
        self.manager.current = 'manual_page'

#class for the settings page
class SettingsPage(Screen):
    
    #constructor for the settings page
    def __init__(self, **kwargs):
        super(SettingsPage, self).__init__(**kwargs)
        layout = BoxLayout(orientation='vertical')

        #add a label to the screen
        self.label = Label(text= "Welcome to the Settings Page", font_size=20)
        layout.add_widget(self.label)
        
        #make a button to go to the home page
        button_home = Button(text = 'go to home page')
        button_home.bind(on_press = self.go_to_home_page)
        layout.add_widget(button_home)

        self.add_widget(layout)
    
    #code to go back to the front page 
    def go_to_home_page(self, instance):
        self.manager.current = 'front_page'

#class for the camera page
class CameraPage(Screen):

    #constructor for the camera page
    def __init__(self, **kwargs):
        super(CameraPage, self).__init__(**kwargs)
        layout = BoxLayout(orientation='vertical')

        #add a label to the screen
        self.label = Label(text= "Welcome to the Camera Page", font_size=20)
        layout.add_widget(self.label)
       
        #make a button to go to the home page
        button_home = Button(text = 'go to home page')
        button_home.bind(on_press = self.go_to_home_page)
        layout.add_widget(button_home)
        self.add_widget(layout)

    #code to go back to the front page
    def go_to_home_page(self, instance):
        self.manager.current = 'front_page'     

#class for the manual page
class ManualPage(Screen):

    #constructor for the manual page
    def __init__(self, **kwargs):
        super(ManualPage, self).__init__(**kwargs)
        layout = BoxLayout(orientation='vertical')

        #add a label to the screen
        self.label = Label(text= "Welcome to the Manual Page", font_size=20)
        layout.add_widget(self.label)

        #make a button to go to the home page
        button_home = Button(text = 'go to home page')
        button_home.bind(on_press = self.go_to_home_page)
        layout.add_widget(button_home)
        self.add_widget(layout)
    
    #code to go back to the front page
    def go_to_home_page(self, instance):
        self.manager.current = 'front_page'

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


# Hyperfix8 Capstone
This readme file was generated on 10-8-2024 by Carlos Garcia

GENERAL INFORMATION

Name: Andres Montoya
EUID: amm0810
Institution: University of North Texas
Email: andresmontoya@my.unt.edu

Andres Montoya -> CryoPan

Name: Abel Montoya
EUID: am1624
Instutution: University of North Texas
Email. abelmontoya@my.unt.edu

Abel Montoya -> AmontTheGreat

Name:Carlos Garcia
EUID:cmg0412
Institution: University of North Texas 
Email:CarlosGarcia9@my.untdallas.edu

Carlos Garcia -> GreenLanturn1796

Name: Joel Hunt
EUID: jh1249
Institution: University of North Texas
Email: joelhunt2@my.unt.edu

Joel Hunt -> joelrhunt

# Instaling Ubuntu virtual machine

https://www.youtube.com/watch?v=DhVjgI57Ino&t=516s

Go through the video step by step to install Ubuntu on the vitrual machine.

# Installing Kivy

1. In Vscode terminal type in the command line "pip install --upgrade pip"
2. In Vscode terminal type in the command line "pip install kivy[full]" to install the full kivy package
3. In Vscode terminal type in the command line "pip install opencv-python"
4. In Vscode terminal type in the command line "pip install plyer"


# installing buildozer and dependencies on Ubuntu linux environment

**Requirements.txt**
    ```
    appdirs==1.4.4
    build==1.2.2.post1
    buildozer==1.5.0
    certifi==2024.8.30
    charset-normalizer==3.4.0
    colorama==0.4.6
    Cython==3.0.11
    distlib==0.3.9
    docutils==0.21.2
    filelock==3.16.1
    idna==3.10
    Jinja2==3.1.4
    Kivy==2.3.0
    Kivy-Garden==0.1.5
    kivymd==1.1.1
    MarkupSafe==3.0.2
    numpy==2.1.3
    opencv-python==4.10.0.84
    packaging==24.2
    pexpect==4.9.0
    pillow==10.4.0
    platformdirs==4.3.6
    plyer==2.1.0
    ptyprocess==0.7.0
    Pygments==2.18.0
    pyjnius==1.5.0
    pyproject_hooks==1.2.0
    python-for-android==2024.1.21
    requests==2.32.3
    sh==1.14.3
    six==1.16.0
    toml==0.10.2
    urllib3==2.2.3
    virtualenv==20.27.1
    ```

1. in ubuntu in a folder for an app install these dependences
    ```
    sudo apt update
    sudo apt install -y python3 python3-venv python3-pip openjdk-11-jdk \
    libffi-dev libssl-dev libjpeg-dev libz-dev gcc g++ make unzip \
    libgl1-mesa-dev libgles2-mesa-dev
    ```

2. Create a virtual environment 
    ```
    python3 -m venv <venv_name>
    source <venv_name>/bin/activate
    ```

3. Upgrade Pip and install Dependencies 
    ```
    pip install --upgrade pip setuptools
    pip install -r requirements.txt
    ```

4. Configure Buildozer
    ```
    buildozer init
    this will generate a file named buildozer.spec which you use to add the requirements and other dependencies needed for the apk/app creation
    need to figure out the exact .spec file needed still troubleshooting this 
    I do know in requirements for opencv dont put 'opencv-python' put 'opencv'
    ``` 

5. debug the files
    ```
    buildozer -v android debug
    **for any large changes to files make sure to run buildozer -v android clean this will make the next build take longer but will ensure the build is correct
    ```


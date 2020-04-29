#!C:\src\rbtools\build\windows-pkg\build\Python27\python.exe
# EASY-INSTALL-ENTRY-SCRIPT: 'RBTools==1.0','console_scripts','rbt'
__requires__ = 'RBTools==1.0'
import re
import sys
from pkg_resources import load_entry_point

if __name__ == '__main__':
    sys.argv[0] = re.sub(r'(-script\.pyw?|\.exe)?$', '', sys.argv[0])
    sys.exit(
        load_entry_point('RBTools==1.0', 'console_scripts', 'rbt')()
    )

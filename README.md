# nb-sonarlint
A SonarLint plugin for Netbeans

It runs SonarLint in connected mode when a file is saved or opened and it shows the issues as editor annotations.

To see the rule details, put the caret position in the line with a issue and choose the item **Show Rule Details** in the contextual menu.

## Global Settings ##

The SonarQube server url (default is http://localhost:9000) and user token can be set in **Tools > Options > Miscellaneous > SonarQube**. 

## Per Project Properties ##

SonarLint can be enabled or disabled per project in **Properties > SonarLint**. A list of excluded files can also be defined in this section.

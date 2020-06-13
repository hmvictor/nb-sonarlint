# nb-sonarlint
A SonarLint plugin for Netbeans

It runs SonarLint in connected mode when a file is saved or opened and it shows the issues as editor annotations.

To see the rule details, put the caret position in the line with a issue and choose the item **Show Rule Details** in the contextual menu or hold Ctrl key and click over the annotated line.

## Global Settings ##

The SonarQube server url (default is http://localhost:9000) and security user token can be set in **Tools > Options > Miscellaneous > SonarQube**. 

## Per Project Properties ##

SonarLint can be enabled or disabled per project in **Properties > SonarLint** (for example, if the SonarQube server is down). A list of excluded files can also be defined in this section.

## Installation ##

The plan is that after some stabilization, the plugin will be in the Netbeans plugin portal and in the Update center. Meanwhile, you can compile the plugin with maven (execute **mvn install**, plugin file with the nbm extension will be generated in the target directory) and do a [manual installation](http://wiki.netbeans.org/InstallingAPlugin).

The project was made with Java 11.

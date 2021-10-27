"""
Script to prepare translation files

 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
"""

import os
import json

i = 0
all_words = []
for path, directories, files in os.walk("androidAppTranslation"):
    for file in files:
        if file == "compass.json":
            top_folder = os.path.normpath(path).split(os.sep)[-1]
            
            if top_folder in ["en", "de"]:
                continue

            print(path)
            print(top_folder)

            
            print(top_folder)
                        
            full_path = os.path.join(path, file)
            
            cc_map_rev = {
              "en": "values",
              "de": "values-de-rAT",
              "bg": "values-bg-rBG",
              "et": "values-et-rEE",
              "mk": "values-mk-rMK",
              "ro": "values-ro-rRO",
            }
            
            country_folder = cc_map_rev[top_folder]

            with open(full_path, "r", encoding="utf-8") as f:
                json_dict = json.load(f)
            print(json_dict)
            
            with open("../app/src/main/res/" + country_folder + "/" + "strings.xml", "w", encoding="utf-8") as fo:    
                fo.write("""<?xml version="1.0" encoding="utf-8"?>
<!--
  ~ COMPASS orienteering game
  ~ Copyright (C) 2021 University of Vienna
  ~ This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
  ~ This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
  ~ You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
  -->

<resources>
    <string name="app_name" translatable="false">COMPASS</string>
    <string name="cp_name_regular" translatable="false">#%1$d</string>
    <string name="notification_title" translatable="false">COMPASS</string>

""")
                for key in json_dict:
                    fo.write("    <string name=\"" + key + "\">" + json_dict[key] + "</string>\n")
                fo.write("</resources>")
                                   
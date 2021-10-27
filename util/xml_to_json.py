"""
Script to prepare translation files

 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
"""

import os
import xmltodict
import json

i = 0
all_words = []
for path, directories, files in os.walk("../app/src/main/res"):
    for file in files:
        if file == "strings.xml":
            print(path)
            print(os.path.normpath(path).split(os.sep))
            top_folder = os.path.normpath(path).split(os.sep)[-1]
            print(top_folder)
            if i == 0:
                assert(top_folder == "values")  # quick and dirty assumption: first file contains all words...               
            
            json_dict = {}
            full_path = os.path.join(path, file)
            
            cc_map = {
              "values": "en",
              "values-de-rAT": "de",
              "values-bg-rBG": "bg",
              "values-et-rEE": "et",
              "values-mk-rMK": "mk",
              "values-ro-rRO": "ro",
            }
            
            country_code = cc_map[top_folder]
            
            if country_code in ["en", "de"]:
                with open(full_path, "r", encoding="utf-8") as f:
                    content = f.read()
                output = xmltodict.parse(content)
            else:
                output = all_words
            print(len(output["resources"]["string"]))
            for dictionary in output["resources"]["string"]:
                if "@translatable" in dictionary and dictionary["@translatable"] == "false":
                    print("Skipped untranslatable entry", dictionary)
                    continue
                name = dictionary["@name"]
                if country_code in ["en", "de"]:
                    text = dictionary["#text"]
                else:
                    text = ""
                json_dict[name] = text
                

            with open("json-translate/" + country_code + "/compass.json", "w", encoding='utf8') as json_file:
                json.dump(json_dict, json_file, ensure_ascii=False)

                        
            if i == 0:
                all_words = output

            i += 1
            
;
; Copyright © 2021, 2023 Peter Monks, JohnnyJayJay
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;
; SPDX-License-Identifier: Apache-2.0
;
; IMPORTANT NOTICE:
; This file has been modified by JohnnyJayJay for a fork of the original project.
; The modifications found in this file are made available under the same license as the original (Apache-2.0).
; The modifications include:
; - Addition of new functions
; - Adjustments of parameter formats of existing functions
; - Additional logic within the code
; - Updated documentation strings.
;
; The original file was obtained from https://github.com/pmonks/tools-licenses.
; The forked version can be found at https://github.com/JohnnyJayJay/lein-licenses.

(ns leiningen.check-asf-policy
  (:require [asf-cat.api :as asf]
            [clojure.pprint :as pp]
            [lice-comb.deps :as lcd]
            [lice-comb.files :as lcf]
            [leiningen.licenses :as lic]))

(defn check-asf-policy
  "Checks your project's dependencies against the ASF's 3rd party license policy (https://www.apache.org/legal/resolved.html).

  format  -- opt: output format, one of :summary, :detailed, :edn (defaults to :summary)

  Note: has the side effect of 'prepping' your project with its transitive dependencies (i.e. downloading them if they haven't already been downloaded)."
  ([project out-format]
   (let [lib-map                  (lic/prep-project project)
         proj-licenses            (lcf/dir->ids ".")
         dep-licenses-by-category (group-by #(asf/least-category (:lice-comb/licenses (val %))) (lcd/deps-licenses lib-map))]
     (when-not (seq (filter #(= "Apache-2.0" %) proj-licenses))
       (println "Your project is not Apache-2.0 licensed, so this report will need further investigation.\n"))
     (case (read-string out-format)
       :summary  (do
                   (println "Category                       Number of Deps")
                   (println "------------------------------ --------------")
                   (doall
                    (map (fn [category]
                           (let [category-info (get asf/category-info category)]
                             (println (format "%-30s %d" (:name category-info) (count (get dep-licenses-by-category category))))))
                         asf/categories))
                   (println "\nFor more information, please see https://github.com/pmonks/tools-licenses/wiki/FAQ"))
       :detailed (do
                   (doall
                    (map (fn [category]
                           (let [category-info (asf/category-info category)
                                 dep-licenses  (seq (get dep-licenses-by-category category))]
                             (when dep-licenses
                               (let [dep-licenses (apply hash-map (flatten dep-licenses))]
                                 (println (str (:name category-info) ":"))
                                 (doall
                                  (map #(println "  *" (lic/dep-and-licenses % (sort asf/license-comparator (:lice-comb/licenses (get dep-licenses %)))))
                                       (sort (keys (get dep-licenses-by-category category)))))
                                 (println)))))
                         asf/categories))
                   (println "For more information, please see https://github.com/pmonks/tools-licenses/wiki/FAQ"))
       :edn      (pp/pprint dep-licenses-by-category))))
  ([project]
   (check-asf-policy project ":summary")))

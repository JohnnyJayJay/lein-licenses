;
; Copyright © 2021 Peter Monks
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

(ns leiningen.licenses
  "Leiningen tasks related to dependency licenses."
  (:require [clojure.string          :as s]
            [clojure.pprint          :as pp]
            [clojure.java.io         :as io]
            [clojure.tools.deps      :as d]
            [lice-comb.deps          :as lcd]
            [lice-comb.files         :as lcf]
            [asf-cat.api             :as asf]))

(defn- lein-dep->deps-dep
  "Translate leiningen dependency vector to Clojure deps key-value pair"
  [global-exclusions [name version & {:keys [classifier exclusions extension]}]]
  [(symbol (str name (when classifier (str \$ classifier))))
   {:mvn/version version
    :exclusions (->> exclusions
                     (concat global-exclusions)
                     (map #(cond-> % (sequential? %) first true symbol))
                     dedupe
                     vec)
    :deps/manifest extension}])

(defn- lein-repo->deps-repo
  "Translate leiningen repository descriptor to Clojure deps repository key-value pair"
  [[name info]]
  (if (map? info)
    (let [general-opts (select-keys info [:checksum :update])
          snapshot-opts (cond-> general-opts (not (:snapshots info)) (assoc :enabled false))
          release-opts (merge general-opts (:releases info))]
      [name {:url (:url info)
             :releases release-opts
             :snapshots snapshot-opts}])
    [name {:url info}]))

(defn- prep-project
  "Prepares the project and returns the lib-map for it."
  [{:keys [dependencies local-repo repositories exclusions]}]
  (let [basis {:deps (->> dependencies (map (partial lein-dep->deps-dep exclusions)) (into {}))
               :mvn/repos (->> repositories (map lein-repo->deps-repo) (into {}))
               :mvn/local-repo local-repo}
        lib-map (d/resolve-deps basis {})
        _       (d/prep-libs! lib-map {:action :prep :log :info} {})]  ; Make sure everything is "prepped" (downloaded locally) before we start looking for licenses
    lib-map))

(defn- dep-and-licenses
  [dep licenses]
  (str dep " [" (s/join ", " licenses) "]"))

(defn licenses
  "Lists all licenses used transitively by the project.

  project   -- Leiningen project map
  format    -- output format, one of :summary, :detailed, :edn. If omitted defaults to :summary

  Note: has the side effect of 'prepping' your project with its transitive dependencies (i.e. downloading them if they haven't already been downloaded)."
  ([project format]
   (let [lib-map       (prep-project project)
         proj-licenses (lcf/dir->ids ".")
         dep-licenses  (lcd/deps-licenses lib-map)
         fq-project-name (symbol (:group project) (:name project))]
     (case format
       :summary  (let [freqs    (frequencies (filter identity (mapcat :lice-comb/licenses (vals dep-licenses))))
                       licenses (seq (sort (keys freqs)))]
                   (print "This project: ")
                   (if (seq proj-licenses)
                     (println (s/join ", " (sort proj-licenses)))
                     (println "- no licenses found -"))
                   (println "\nLicense                                  Number of Deps")
                   (println "---------------------------------------- --------------")
                   (if licenses
                     (doall (map #(println (format "%-40s %d" % (get freqs %))) licenses))
                     (println "  - no licenses found -")))
       :detailed (let [direct-deps     (into {} (remove (fn [[_ v]] (seq (:dependents v))) dep-licenses))
                       transitive-deps (into {} (filter (fn [[_ v]] (seq (:dependents v))) dep-licenses))]
                   (println "This project:")
                   (if proj-licenses
                     (println "  *" (dep-and-licenses fq-project-name (sort proj-licenses)))
                     (println "  - no licenses found -"))
                   (println "\nDirect dependencies:")
                   (if direct-deps
                     (doall (for [[k v] (sort-by key direct-deps)] (println "  *" (dep-and-licenses k (:lice-comb/licenses v)))))
                     (println "  - no direct dependencies -"))
                   (println "\nTransitive dependencies:")
                   (if transitive-deps
                     (doall (for [[k v] (sort-by key transitive-deps)] (println "  *" (dep-and-licenses k (:lice-comb/licenses v)))))
                     (println "  - no transitive dependencies -")))
       :edn      (pp/pprint (into {fq-project-name {:this-project true :lice-comb/licenses proj-licenses :paths [(.getCanonicalPath (io/file "."))]}}
                                  dep-licenses)))
     (let [deps-without-licenses (seq (sort (keys (remove #(:lice-comb/licenses (val %)) dep-licenses))))]
       (when deps-without-licenses
         (println "\nUnable to determine licenses for these dependencies:")
         (doall (map (partial println "  *") deps-without-licenses))
         (println "\nPlease raise an issue at https://github.com/pmonks/lice-comb/issues/new?assignees=pmonks&labels=unknown+licenses&template=Unknown_licenses_tools.md and include this list of dependencies.")))))
  ([project]
   (licenses project :summary)))

(defn check-asf-policy
  "Checks your project's dependencies against the ASF's 3rd party license policy (https://www.apache.org/legal/resolved.html).

  format  -- opt: output format, one of :summary, :detailed, :edn (defaults to :summary)

  Note: has the side effect of 'prepping' your project with its transitive dependencies (i.e. downloading them if they haven't already been downloaded)."
  ([project format]
   (let [lib-map                  (prep-project project)
         proj-licenses            (lcf/dir->ids ".")
         dep-licenses-by-category (group-by #(asf/least-category (:lice-comb/licenses (val %))) (lcd/deps-licenses lib-map))]
     (when-not (seq (filter #(= "Apache-2.0" %) proj-licenses))
       (println "Your project is not Apache-2.0 licensed, so this report will need further investigation.\n"))
     (case format
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
                                  (map #(println "  *" (dep-and-licenses % (sort asf/license-comparator (:lice-comb/licenses (get dep-licenses %)))))
                                       (sort (keys (get dep-licenses-by-category category)))))
                                 (println)))))
                         asf/categories))
                   (println "For more information, please see https://github.com/pmonks/tools-licenses/wiki/FAQ"))
       :edn      (pp/pprint dep-licenses-by-category))))
  ([project]
   (check-asf-policy project :summary)))

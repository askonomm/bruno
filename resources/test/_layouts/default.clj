(declare ^:dynamic post)
(declare ^:dynamic partial)

[:div
 (partial "header" {'test "this"})
 [:div.post
  [:h2 (:title post)]
  [:div.entry (:entry post)]]]
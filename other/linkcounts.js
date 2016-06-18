get_query_object: function (query, get_filters, applied_filters) {
          // query: string
          // get_filters: boolean
          // applied_filters: null or list of applied filters (in format supplied by Filter_Tree...)
          var core_query = {
              "query_string": {
                  "query": query.replace(/(\S)"(\S)/g, '$1\u05f4$2'), //Replace internal quotes with gershaim.
                  "default_operator": "AND",
                  "fields": ["content"]
              }
          };

          var o = {
              "sort": [{
                  "order": {}                 // the sort field name is "order"
              }],
              "highlight": {
                  "pre_tags": ["<b>"],
                  "post_tags": ["</b>"],
                  "fields": {
                      "content": {"fragment_size": 200}
                  }
              }
          };

          if (get_filters) {
              //Initial, unfiltered query.  Get potential filters.
              o['query'] = core_query;
              o['aggs'] = {
                  "category": {
                      "terms": {
                          "field": "path",
                          "size": 0
                      }
                  }
              };
          } else if (!applied_filters) {
              o['query'] = core_query;
          } else {
              //Filtered query.  Add clauses.  Don't re-request potential filters.
              var clauses = [];
              for (var i = 0; i < applied_filters.length; i++) {
                  clauses.push({
                      "regexp": {
                          "path": RegExp.escape(applied_filters[i]) + ".*"
                      }
                  })
              }
              o['query'] = {
                  "filtered": {
                      "query": core_query,
                      "filter": {
                          "or": clauses
                      }
                  }
              };
          }
          return o;
      }

//EXPECT A RESULT OF FILTERS LIKE THIS

{  
   "aggregations":{  
      "category":{  
         "doc_count_error_upper_bound":0,
         "sum_other_doc_count":0,
         "buckets":[  
            {  
               "key":"Halakhah/Tur and Commentaries/Bach",
               "doc_count":2
            },
            {  
               "key":"Halakhah/Tur and Commentaries/Beit Yosef",
               "doc_count":1
            }
         ]
      }
   }
}
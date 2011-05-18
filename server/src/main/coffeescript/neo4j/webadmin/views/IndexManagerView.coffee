###
Copyright (c) 2002-2011 "Neo Technology,"
Network Engine for Objects in Lund AB [http://neotechnology.com]

This file is part of Neo4j.

Neo4j is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
###

define( 
  ['neo4j/webadmin/templates/indexmanager/base',
   'neo4j/webadmin/templates/indexmanager/indexes',
   'neo4j/webadmin/views/indexmanager/IndexView'
   'neo4j/webadmin/views/View',
   'lib/backbone'], 
  (template, indexTemplate, IndexView, View) ->

    class IndexManagerView extends View
      
      template : template
      
      events : 
        "click .create-index" : "createIndex"
     
      initialize : (opts) =>
        @appState = opts.state
        @server = @appState.getServer()
        @idxMgr = opts.idxMgr
        @idxMgr.bind("change", @renderIndexList)

      render : =>
        $(@el).html(template())
        @renderIndexList()        
        return this
        
      renderIndexList : =>
        indexList = $("#indexes", @el)
        for index in @idxMgr.get "indexes" 
          indexList.append( new IndexView({index : index, idxMgr : @idxMgr}).render().el )
      
      createIndex : => 
        alert($("#create-index-name").val())
        @idxMgr.createIndex({name : $("#create-index-name").val()})
)
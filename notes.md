(personal working notes and brainstorms)
    
# next actions

- can I build code and jar from command line? (exercise to help me understand what is going on)
- skim docs/tutorials on streams?
    
# roadmap

Iteration #1 - (DONE) minimal features so I use it instead of Python one

- x make Receive bigger so it is easy to click
- x double click to receive item
- x two store modes:
    - x by append and by replace (what does this look like with multiselect?)
    - x separate buttons? right click button to change mode? checkbox somehwere? toggle in Settings menu item?)
- x delete selected
- x add menus
    - x file
        - x exit
    - x clip
        - x deleted selected
- x truncate text display for very long items or ones with embedded cr's?
	
	
# use cases

- x create clips
    - x read form clipboard, x file, x url, x list of files
- managing and mutating clip buffer
    - x append, x replace, x remove, reorder, x sort, x clear, x filter, x remove empty lines (special case of filter), x remove duplicates/uniqueify, x export to disk
    - x able to apply mutations to individual clips or to all selected clips?
- mutating clip contents
    - string
        - x trim, x lower, x upper, x prepend, x append, x split, x variable substitute
    - list
        - x prepend, x append, x join, x filter, x reverse, x sort, x count, numbering (prepend and append) with start # and step # w/ tooltip explaining use of args
        - x substring trimming (cut off first character [1:], return last three characters [-3:], return 3rd character [3])
        - x Apply Python style slicing syntax on each item in the list
        - Apply Python style slicing syntax on the list as a whole to filter out certain lines
    - table
        - sort, filter, reorder columns
        - mimic the unix "cut" syntax, maybe? Can I use this tool to split on a delimiter and reorder columns (get the behavior of: cut -d',' -f3,1,4,2)
- retrieving clips
    - x single
    - x multiple selected
        - x cycle through all/selected?
        - x join w/ provided delimiter (joins with \n, but can do a replace next)
- destination of receive
    - x clipboard, x file, post to url, substitute into shell command?
    
uncategorized
    
- java clipboard toolbox UI
    - able to x split, x join, x prepend, x append, x replace, x sort, x filter, columns, graph, x clipboard stack, etc.
    - x variable substitution?
    - x push and retrieve from other buffers
    - able to treat buffers and deque (push/pop from front and back) and random access (read from anywhere)
    - x clipboard stack/cache/manipulator
    - x manage a clipboard stack
    - x allow modifications (regex, substitution, replacement, stripping, append/prepend, numbering) to clipboard in memory and clipboard buffers
    - different ways of getting text in (x take a multi-line selection and put each line into a separate bucket? separate by a comma?)
        - drag and drop replaces
        - x drag and drop appends (maybe with optional delimiter)
    - different ways of getting text out (clipboard, save to file, POST to URL?)
        - post clipboard to url
        - use variable substitution to open a url (ex: http://www.stuff.com/doit/%{1}
    - x variable substitution
        - x be able to independently set values for aaa=somestring bbb=other and substitute those into a sequence of clips 
(if clip contains "this is ${aaa} for ${bbb}". 
    - open url after substituting current clipboard into a url (ex: http://docs.oracle.com/javase/8/docs/api/javax/swing/SpinnerDateModel.htm
l (any way to search for docs given just a class/interface name?)
    - buffer
        - x append/prepend system clipboard to selected buffers (if I want to incrementally build up a long clipboard buffer entry)
        - x replace
        - insert after selected (don't want to do "before selected" because then I could never insert anything at the end of the list. The combination of "after selected" and "store at head" lets me put stuff anywhere
        - x reorder
        - x variable substitution that can reference other buffers (by index? indexes can change easily. Some tagging mechanism by color? icon? assigned number? initial text?)

# modification
- what is input? output?

- clip operations (modifies selected clip)
    - x prepend, x append
        - in: string 
        - output: selected clip
- line operations (treats selected clip as a list of lines with each line being one item)
    - x sort lines (tokenizes by lines)
        - in: selected clip
        - out: selected clip
    - x filter lines (tokenize by lines)

# clipboard todo
- maybe list out core use cases (broad, but not exhaustive)
    - list bare-bones features to replace my python implmenetation so i will actually start using it
    - then list ist *must* have features (the core, fun features like append, split, join, sort filter)
    - have a second list for "maybe"

- x double click on item does a "retrieve" of that item into clipboard
    - what about multi-select?
- with multi-select, maybe retrieve becomes multi-select aware, where it merges the multiple lines together with a given token (empty, space, carriage return, tab, comma)
    - how do i reconcile this with doing a multi-select to be used in a cyclical retrieve?  THe assumption would be that a cyclical retrieve would be bounded by what is selected? Maybe two modes (merge and cycle) of retrieve? When in cycle mode, how does UI track what clip is current?
     

# Feature details

### Cycling through clipboard buffer
 
Visualizing focus ideas as when the ListView control as a whole looses focus, the individual cell focus (thin border) is not shown. So, what are other ways I can visualize the current cell while cycling?  Here are brainstorms:

- force the control focus to always be on the ListView, which shows the selected items and draws a border around the item with focus.
- create global css file for UI that still shows the focused cell item even if the ListView doesn't have the focus.  When the ListView doesn't have the focus, it uses gray instead of blue and no border around focused item (probably because three is no focus on the ListView!). This snippet is a starting point, but doesn't do what I want.
    - `items.setStyle("-fx-selection-bar: red; -fx-selection-bar-non-focused: blue; -fx-selection-bar-text: black");`

- do some manual animation on the cell to highlight it
- add some text to the data model if it has focus
- have another control (another ListView) next to it in parallel that highlights the current cycle row somehow?
- add a status bar at the bottom of the screen and have it show the buffer ID with the focus and preview of it
- just not worry about having a nice visualization for cycling for now

# Brainstorm

- some kind of preview of system clipboard (have it poll the system clipboard while UI is open)
- Feature that would provide a full screen view of the current system clipboard. Maybe just a "open in Notepad" would be sufficient.
- x diff two buffers. Diff a buffer with system clipboard.
- maybe have a generic "store" button that has different behavior based on some mode/configuration?
    - append new clip to buffer?
    - prepend/append/replace selected clips in buffer with text in system clipboard?
        - do arbitrary multation operations to buffers (able to achieve this with multiselect->join->listPrepend->DelSelBuffers->ListStore)
- clear the status bar x seconds after the event happens. This means that if you are retriving the same string over and over, you don't see any change on the status bar "Retrieving 10 chars..." and you don't get a visual indication an event has happened.  But if the status disappars, it is more visually obvious when something changes.
    - bonus: make it fade out
    - bonus: maybe don't remove status messages, but have new status bar entries flash or pulse to show they are new?
- look at my enso plugins or java.util.Collections for ideas of other string actions
- sorting based on a substring (tokenizing line for a table-like sort, substring, etc.))
- numeric sort
- list of values
    - min, max, average, sum, stddev
    - visualization
        - line/bar charts (maybe start with text based ASCII art visualizations to start and move to actual graphics later). Remember you can easily copy a list of numbers into Excel.
        - histograms
    - calculator of sorts where i can define a function (x*2)+x and feed a list of numbers through it
- configuration
    - for path to diff tool and notepad
- drag and drop implementation for reordering ListView contents https://coderanch.com/t/658527/java/Implement-drag-drop-ListView-custom
- x in addition to filtering, have something that counts how many times a given regex matches? Or, maybe just use the "filter" and then the "stats" to count the lines in the clipboard after the filtre.
- x center, x wordwrap
- use case test: can i use the features I have to wrap html tags around a list of text?
- Thinking about visual flow and how to present the argument fields for each action (which have a lot of variety)
    - problem:
        - too confusing where you have some buttons that use arg fields
            - don't know what args do
            - don't know what args go with what buttons.
            - extra arg boxes are confusing
            - don't have good instructions on what each button does.
    - problems
        - moving the mouse from buttons down to args means you cross other buttons, which changes what args are visible.
    - brainstorm
        - visual flow... decision tree that flows from top to bottom.  that is how your eye moves.
        - have all buttons in one horizontal row, so as you move vertically down, you won't cross other buttons. Organize each set of buttons in a tab
    - bits
        - hover over/out events
        - have data that, per button, maps what widgets need to be disabled and what its help text is
            - maybe eventually animate arguments on and off
            - maybe remember the history of entries for that widget and apply that history
            
# Bugs

- lots of silent failures in drag-n-drop files/directories on read failures (file being an executable, for instance). It silently skips the file if it fails.
- if you type multi-line strings into the text box in ClipDashboard and copy into the system clipboard, the lines are separated with "\n", not the expected "\r\n". Might need to replace \n's that don't have \r with "\r\n" to normalize all text when reading from the clipboard. 
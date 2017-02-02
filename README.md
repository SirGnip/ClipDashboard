# roadmap

Iteration #1 - minimal features so I use it instead of Python one

- x make Receive bigger so it is easy to click
- x double click to receive item
- x two store modes:
    - x by append and by replace (what does this look like with multiselect?)
    - x separete buttons? right click button to change mode? checkbox somehwere? toggle in Settings menu item?)
- x delete selected
- x add menus
    - x file
        - x exit
    - x clip
        - x deleted selected
- truncate text display for very long items or ones with embedded cr's?
		
# use cases

- create clips
    - read form clipboard, file, url, list of files
- managing and mutating clip buffer
    - x append, x replace, x remove, reorder, sort, x clear, filter, remove empty lines (special case of filter), remove duplicates/uniqueify, export to disk
    - x able to apply mutations to individual clips or to all selected clips?
- mutating clip contents
    - string
        - x trim, x lower, x upper, x prepend, x append, x split, variable substitute
    - list
        - x prepend, x append, x join, filter, reverse, sort, x count, numbering (prepend and append) with start # and step # w/ tooltip explaining use of args
- retrieving clips
    - x single
    - multiple selected
        - cycle through all/selected?
        - join w/ provided delimiter
- destination of receive
    - x clipboard, file, post to url, substitute into shell command?
    
uncategorized
    
- java clipboard toolbox UI
    - able to split, join, prepend, append, replace, sort, filter, columns, graph, clipboard stack, etc.
    - variable substitution?
    - push and retrieve from other buffers
    - able to treat buffers and deque (push/pop from front and back) and random access (read from anywehre)
    - clipboard stack/cache/manipulator
    - manage a clipboard stack
    - allow modifications (regex, substitution, replacement, stripping, append/prepend, numbering) to clipboard in memory and 
clipboard buffers
    - different ways of getting text in (take a multi-line selection and put each line into a separate bucket? separate by a comma?)
        - drag and drop replaces
        - drag and drop appends (maybe with optional delimiter)
    - different ways of getting text out (clipboard, save to file, POST to URL?)
        - post clipboard to url
        - use variable substitution to open a url (ex: http://www.stuff.com/doit/%{1}
    - variable substitution
        - be able to independently set values for aaa=somestring bbb=other and substitute those into a sequence of clips 
(if clip contains "this is ${aaa} for ${bbb}". 
    - open url after substituting current clipboard into a url (ex: http://docs.oracle.com/javase/8/docs/api/javax/swing/SpinnerDateModel.htm
l (any way to search for docs given just a class/interface name?)
    - buffer
        - x append/prepend system clipboard to selected buffers (if I want to incrementally build up a long clipboard buffer entry)
        - x replace
        - insert after selected (don't want to do "before selected" because then I could never insert anything at the end of the list. The combinaton of "after selected" and "store at head" lets me put stuff anywhere
        - reorder
        - variable substitution that can reference other buffers (by index? indexes can change easily. Some tagging mechanism by color? icon? assigned number? initial text?)

# modification
- what is input? output?

- clip operations (modifies selected clip)
    - prepend, append
        - in: string 
        - output: selected clip
- line operations (treats selected clip as a list of lines with each line being one item)
    - sort lines (tokenizes by lines)
        - in: selected clip
        - out: selected clip
    - filter lines (tokenize by lines)

# clipboard todo
- maybe list out core use cases (broad, but not exhaustive)
    - list bare-bones features to replace my python implmenetation so i will actually start using it
    - then list ist *must* have features (the core, fun features like append, split, join, sort filter)
    - have a second list for "maybe" (like ability to control truncation of )

- double click on item does a "retrieve" of that item into clipboard
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
- have another control (anothre ListView) next to it in parallel that highlights the current cycle row somehow?
- add a status bar at the bottom of the screen and have it show the buffer ID with the focus and preview of it
- just not worry about having a nice visualization for cycling for now

# Brainstorm

- some kind of preview of system clipboard (have it poll the systme clipboard while UI is open)
- Feature that would provide a full screen view of the current system clipboard. Maybe just a "open in Notepad" would be sufficient.
- diff two buffers. Diff a buffer with system clipboard.
- maybe have a generic "store" button that has different behavior based on some mode/configuration?
    - append new clip to buffer?
    - prepend/append/replace selected clips in buffer with text in system clipboard?
- clear the status bar x seconds after the event happens. This means that if you are retriving the same string over and over, you don't see any change on the status bar "Retrieving 10 chars..." and you don't get a visual indication an event has happened.  But if the status disappars, it is more visually obvious when something changes.
    - bonus: made it fade out
    - bonus: maybe don't remove status messages, but have new status bar entries flash or pulse to show they are new?
- look at my enso plugins or java.util.Collections for ideas of other string actions


# Bugs

- If you put list-based text into the clipboard (ex: "\n\n\n a \n b \n c \n\n\n") and do a "list trim", it seems to remove the trailing blank rows (but not the initial))... I'm expecting those trailing \n should result in blank rows, even after a trim.  The trimming shouldn't remove lines, only the whitespace from the lines.

              
    
    
        


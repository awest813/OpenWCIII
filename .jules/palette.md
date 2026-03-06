## 2024-05-20 - Java Swing Menu Mnemonics
**Learning:** Warsmash relies heavily on Java Swing for its desktop editor tools (like YseraPanel and TerrainEditorPanel). A common accessibility oversight in these tools is the lack of keyboard shortcuts (mnemonics) for standard top-level menus and common actions (like "File" and "Open").
**Action:** When improving UI accessibility in Java desktop apps within this codebase, always check if JMenu and JMenuItem instances have mnemonics set using `setMnemonic(KeyEvent.VK_...)` to enable `Alt+Key` navigation.

# TaskFlow

A command-line task manager written in Java. It always knows what you should work on next, lets you search as you type, and can undo mistakes.

I built it to get hands-on with core data structures by putting them in something I'd actually use day to day. Instead of reaching for `PriorityQueue` and friends, the three core structures (a binary heap, a trie, and a linked-list stack) are written from scratch and covered by unit tests.

```
> add Renew car registration -p high -d tomorrow
Added #1: Renew car registration
> add "Book dentist appointment" -d +2
Added #2: Book dentist appointment
> add Pay phone bill -p high -d 2026-10-15
Added #3: Pay phone bill
> list
   ID  PRIORITY  DUE          TITLE
!    3  HIGH      2026-10-15   Pay phone bill
     1  HIGH      2026-10-22   Renew car registration
     2  MEDIUM    2026-10-23   Book dentist appointment
> search den
   ID  PRIORITY  DUE          TITLE
     2  MEDIUM    2026-10-23   Book dentist appointment
> delete 2
Deleted: Book dentist appointment  (type 'undo' to bring it back)
> undo
Undid: delete #2
```

`!` marks overdue tasks, `x` marks finished ones.

## Features

- **Smart ordering** – tasks are sorted by priority, then by due date, so `next` always gives the most urgent thing
- **Prefix search** – `search reg` finds "Renew car registration"; `search re` would find it too
- **Undo** – undo adds, edits, completions and deletes, up to the last 50 changes
- **Saves automatically** – tasks are stored in a CSV file and reloaded on startup
- **Friendly dates** – `today`, `tomorrow`, `+3`, or `2026-11-03`
- **Overdue warning** when you open the app

## Running it

Needs Java 17+ and Maven.

```bash
mvn package
java -jar target/taskflow-1.0.0.jar
```

Tasks are saved to `~/.taskflow/tasks.csv`. To use a different file:

```bash
java -jar target/taskflow-1.0.0.jar my-tasks.csv
```

Run the tests with `mvn test`.

## Commands

| Command | What it does |
|---|---|
| `add <title> [-p high\|medium\|low] [-d date]` | Add a task (priority defaults to medium) |
| `list` / `list all` / `list done` / `list overdue` | Show tasks |
| `next` | Show the most urgent unfinished task |
| `done <id>` / `reopen <id>` | Mark a task finished or unfinished |
| `edit <id> [-t title] [-p priority] [-d date\|none]` | Change a task |
| `delete <id>` | Remove a task |
| `search <text>` | Find tasks where a word starts with the text |
| `undo` | Undo the last change |
| `stats` | Totals, % done, and overdue count |

Titles can be quoted (`add "Fix -p bug"`) if they contain something that looks like a flag.

## How it works

`TaskManager` keeps three structures in sync:

| Structure | Used for | Cost |
|---|---|---|
| `HashMap<Integer, Task>` | Looking up a task by id | O(1) |
| `MinHeap<Task>` (custom) | Keeping unfinished tasks in urgency order | add/remove O(log n), peek O(1) |
| `Trie` (custom) | Prefix search over every word in every title | O(k) for a k-letter prefix |
| `LinkedStack<Change>` (custom) | Undo history, capped at 50 | push/pop O(1) |

A few decisions worth mentioning:

**Keeping the heap valid on edits.** If you change a task's priority while it's sitting inside the heap, the heap's ordering is silently broken. So every change goes through one method that removes the task from all three structures, applies the change, and puts it back. If the change is invalid (like an empty title), the original task is restored.

**Undo with snapshots.** Before each change, the manager saves a copy of the task as it was (or `null` if it's a new task). Undoing means removing the current version and putting the copy back. That one rule handles add, edit, complete, reopen and delete without a separate undo method for each.

**The trie stores ids at every node.** Each node remembers which tasks have a word passing through it, so a search walks down the prefix and returns that node's set directly, without searching the subtree below. The tradeoff is more memory per node, which is fine at the scale of a to-do list.

**Safe saves.** The CSV is written to a temp file and then renamed over the real one, so if the program crashes mid-save your old data is still there. Titles are quoted and escaped so commas and quotes in titles don't break the file.

**Testable CLI.** `App` takes its input, output and clock as constructor arguments, so the tests can run whole sessions with scripted input and a fixed date.

## Project layout

```
src/main/java/com/rayn/taskflow/
  Main.java               entry point
  cli/                    App (prompt loop), CommandLine (parsing), DateInput
  service/TaskManager     core logic
  ds/                     MinHeap, Trie, LinkedStack
  model/                  Task, Priority
  storage/CsvTaskStore    saving and loading
src/test/java/...         41 JUnit 5 tests
```

## Ideas for later

- Tags and filtering by tag
- Recurring tasks (every Monday, etc.)
- Fuzzy search so typos still match
- A simple GUI with JavaFX

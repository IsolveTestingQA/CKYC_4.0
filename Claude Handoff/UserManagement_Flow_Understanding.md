# User Management automation - flow as understood and as executed

Author: Aravindhan (Tested by) | Date: 2026-10-13 | Code: `functionality/usermanagement/UsersFunctionality.java`, `RolesFunctionality.java`, `UtilityFiles/UserManagementSession.java`, `UserManagementStore.java`, `UserManagementAccounts.java`

Purpose: confirm the flow with the tester **before** any further change. Everything below is read from the code, nothing is guessed.

## 1. Accounts (TestData/user-management.properties)

| Role in the test | User | Password | Used for |
|---|---|---|---|
| Maker | `automaker` | see user-management.properties (maker.password blank, default used) | Add/Edit/Lock/Unlock/Dormant requests |
| Checker (independent) | `autochecker` | see user-management.properties | Approve / Revert create, edit, role requests |
| Branch Checker | `autochecker` (same account) | see user-management.properties | Approve Lock / Unlock / Dormant |
| Same-role Checker (negative) | `admin` | see user-management.properties | Must NOT be able to approve the Maker's request |
| New user created by the test | `testauto<8 digits>` | see user-management.properties | Login check after approval |

The run signs in as `auto` (config) in the Login scenario, then switches to `automaker` for User Management. One browser, one session: every Maker <-> Checker change is **logout + login**.

## 2. Run order (Test Control Panel)

`Login` -> `Roles Positive` -> `Roles Negative` -> `Users Positive` -> `Users Negative`.
Roles Positive must run first: the role it creates and gets approved is the role chosen on **Add User** (`UserManagementStore.approvedRoleName`, kept across scenarios).

## 3. Roles Positive (feature 8_01)

| # | Who | Action | Pass condition |
|---|---|---|---|
| 1 | Maker | Open User Management > Roles (side nav, URL only as fallback) | Role Management + Add Role visible |
| 2 | Maker | Add Role: name `TEST AUTO ROLE <id>`, code `TA<n>`, description -> Save | Toast has success/submitted/pending **or** role card visible. Queued but **no toast = BUG** (reported) |
| 3 | autochecker | Login -> Roles -> Checker mode -> Pending queue -> find the role (paginate) | Approve/Revert icons for that role present |
| 4 | autochecker | View Details (verify role, maker, type CREATE, status PENDING) -> Approve -> confirm | Approved toast **or** icons gone **or** role in Approved queue |
| 5 | autochecker | Open Approved queue -> View Details again (approved-by shows checker) | Details match |
| 6 | automaker | Configure Permissions on the TEST AUTO role only: tick a child, parent must follow | Parent checkbox selected when child ticked; Save permissions |
| 7 | automaker | Restore maker session on Roles | - |

Super Admin is never opened or edited.

## 4. Roles Negative (feature 8_02)
1. Maker: Add Role -> Save blank -> `helper error count > 0` (mandatory validation).
2. Maker: name `TEST@ROLE`, code `ROLE CODE` -> Save -> helper errors > 0 (format validation).
3. Maker: create a valid TEST AUTO role (queued).
4. Maker: Checker mode -> open the request -> **Approve own request** -> must be rejected (see 7.4).
5. `admin` (same role): login -> Roles -> Pending -> **Approve** -> must be blocked (see 7.4).
6. Restore Maker.

## 5. Users Positive (feature 7_01) - one scenario, 22 steps

| # | Who | Action | Pass condition |
|---|---|---|---|
| 1 | automaker | Side nav > User Management > Users; Maker mode | Maker, Checker, Add User visible |
| 2 | automaker | **Create**: first TEST, last AUTO, designation QA TESTER, email `<login>@example.test`, mobile `98765xxxxx`, login `testauto<n>`, SOL first option, role = approved TEST AUTO ROLE -> Save | Toast success/submitted/pending/saved **or** Approve/Revert visible for the login **or** row text "awaiting approval"/"pending"; missing toast = BUG |
| 3 | autochecker | Login -> Users -> Checker -> Pending -> View Details (login, maker, CREATE, PENDING) -> Approve | See 7.2; if not approved -> same steps with branch checker |
| 4 | autochecker | Login again, Approved queue, View Details: login + maker + CREATE + APPROVED + approved-by (first 4 letters of checker name accepted) | all five found |
| 5 | testauto user | Login with see user-management.properties | Not on `/login`. Still on login = **BUG-003** |
| 6 | automaker | Search the login in Maker grid | Row visible. Skipped (FAIL row "SKIP") if the user was not approved |
| 7 | automaker | **Edit**: designation `QA TESTER EDIT` -> Save | toast success/submitted/pending/updated/saved/lock/dormant |
| 8 | autochecker | Approve the EDIT; verify details (type edit/update) | as step 3-4 |
| 9 | automaker | Edit again: designation `QA TESTER REVERT` -> Save | queued |
| 10 | autochecker | **Revert** the request; verify in Reverted queue | revert toast (revert/returned/maker) **or** icons gone **or** in Reverted queue; Maker still sees the user row |
| 11 | automaker | **Lock** icon on the row -> confirm | toast or pending evidence; `lastPendingAction=LOCK` |
| 12 | autochecker (branch) | Approve LOCK; verify details | prerequisite check: last pending action must equal LOCK |
| 13 | automaker | **Unlock** icon -> confirm | `UNLOCK` queued |
| 14 | autochecker (branch) | Approve UNLOCK; verify details | - |
| 15 | automaker | **Dormant**: Edit -> untick Active (fallback: Status = Dormant) -> Save | `DORMANT` queued; neither control found = FAIL |
| 16 | autochecker (branch) | Approve DORMANT; verify details | - |
| 17 | automaker | Search user: row shows dormant/exit/inactive/locked; list action icons | row exists (text flag preferred) |
| 18 | dormant user | Login with see user-management.properties | **Must fail** (stay on `/login`). Login allowed = BUG |
| 19 | automaker | Restore Maker session on Users | - |

## 6. Users Negative (feature 7_02)
1. Maker: Add User -> Save blank -> `helper error count > 0`.
2. Maker: First Name 51 chars, Email 101 chars + `@x.test`, Login Name 31 chars -> accepted length must be <= 50 / 100 / 30. Then `AB12@`, `not-an-email`, `ABC123` -> Save -> helper errors > 0.
3. Maker: existing login `auto` -> Save -> toast contains already/exist/duplicate **or** helper errors > 0.
4. Maker: create a valid TEST AUTO user (queued).
5. Maker: Checker mode -> Approve own request -> must be rejected.
6. `admin`: login -> Users -> Pending -> Approve -> must be blocked.
7. Restore Maker.

## 7. Conditions used by the code (the part to confirm)

### 7.1 Navigation
Side nav click: open menu -> `User Management` -> `Users`/`Roles`. If the URL does not contain `/admin/users` or `/admin/roles` -> direct URL. Session on login page after switching user = FAIL "Session returned to login".

### 7.2 "Approved" (Users and Roles)
Approval is accepted when ANY of:
- toast text contains `approved` or `success`; or
- after the click neither the Approve nor the Revert icon of that row is present; or
- the row is found in the Approved queue (status dropdown = Approved).
If the Approve icon is missing before the click: accepted when the row is already in the Approved queue; otherwise FAIL "No pending request found".
Primary checker first; branch checker only as fallback if the primary did not approve.

### 7.3 "Queued" (Create/Edit/Lock/Unlock/Dormant by Maker)
Accepted when toast contains any of `success, submitted, pending, updated, saved, lock, dormant` **or** the Approve/Revert icon exists for the login **or** the row says awaiting approval/pending.

### 7.4 Negative approvals (self-approve and same-role)
Pass ("blocked") when ANY of:
- Approve icon is not present at all; or
- redirected to `/login` (reported as BUG-001: no toaster); or
- toast says maker/own/not allowed/cannot/self (self) or same role/role+cannot/not allowed/checker (same-role); or
- toast does not say approved/success.
Fail only when an "approved/success" toast appears.

### 7.5 Reverted
Toast contains revert/returned/maker, **or** both icons gone, **or** row in Reverted queue.

### 7.6 Run data
`UserManagementStore` is cleared at the start of every User Management scenario except the approved role name/code. Only TEST AUTO records are created or changed; seed users and Super Admin are never touched.

## 8. Weak points I see in my own code (to discuss, not yet changed)
1. "Approved" accepts `success` in the toast - "Unsuccessful" would also match. Evidence by icons disappearing can also be a refresh.
2. Negative checks pass when nothing happens (no Approve icon visible, or no approved toast) - they do not prove the application showed a rejection.
3. "Queued" accepts any toast containing `saved`/`updated`/`success`.
4. Same-role negative assumes `admin` and `automaker` have the same role.
5. Dormant check passes when the row merely exists (`hasExitFlag || row exists`).
6. After a failed approval the later steps still run and mostly report SKIP/FAIL, which hides the first real failure.
7. Roles: permission check uses generic checkbox xpaths (`Masters`/`Dashboard` ancestor) - not a stable locator.
8. Timing: toast read waits 3 s; slow Save responses are missed.

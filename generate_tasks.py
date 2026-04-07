import json
import random
from datetime import date, timedelta

statuses = ["OPEN", "IN_PROGRESS", "DONE", "BLOCKED"]
priorities = ["LOW", "MEDIUM", "HIGH"]

title_prefixes = [
    "Prepare", "Review", "Fix", "Update", "Validate", "Design", "Implement",
    "Document", "Test", "Analyze", "Refactor", "Deploy", "Investigate",
    "Optimize", "Create", "Plan", "Monitor", "Check", "Verify", "Improve"
]

title_objects = [
    "login validation", "API response handling", "sprint planning notes",
    "database migration script", "error logging", "user onboarding flow",
    "password reset process", "task dashboard", "notification service",
    "access control rules", "frontend styling", "backend integration",
    "test coverage", "deployment pipeline", "performance issue",
    "security review", "report export", "session timeout handling",
    "admin approval workflow", "input validation"
]

descriptions = [
    "Review the implementation and ensure it matches expected behavior.",
    "Investigate the issue and apply the required fix.",
    "Prepare documentation and verify all edge cases.",
    "Coordinate with team members and update the related components.",
    "Test the feature thoroughly and record the observations.",
    "Improve the current implementation for better reliability.",
    "Validate business rules and confirm correct error handling.",
    "Refine the workflow and ensure status transitions are correct.",
    "Check data persistence and verify database consistency.",
    "Analyze logs and confirm the issue is resolved."
]

tasks = []
start_date = date.today()

for i in range(1000):
    title = f"{random.choice(title_prefixes)} {random.choice(title_objects)}"
    desc = random.choice(descriptions)

    # realistic distribution
    status = random.choices(
        statuses,
        weights=[35, 30, 25, 10],
        k=1
    )[0]

    priority = random.choices(
        priorities,
        weights=[25, 50, 25],
        k=1
    )[0]

    due_date = None
    if random.random() < 0.7:
        due_date = (start_date + timedelta(days=random.randint(1, 90))).isoformat()

    task = {
        "title": title[:140],
        "description": desc[:5000],
        "status": status,
        "priority": priority
    }

    if due_date:
        task["dueDate"] = due_date

    tasks.append(task)

with open("tasks_1000.json", "w", encoding="utf-8") as f:
    json.dump(tasks, f, indent=2)

print("Created tasks_1000.json with 1000 tasks")
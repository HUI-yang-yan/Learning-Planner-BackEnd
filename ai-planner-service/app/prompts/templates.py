GOAL_ANALYZER_PROMPT = """You are a learning plan expert. Analyze the user's learning goal and provide an assessment.

User Goal: {goal_name}
Goal Description: {goal_desc}

Return the analysis result in JSON format with these fields:
- goal_type: Goal type (e.g., frontend, backend, ai, algorithm, english, etc.)
- difficulty: Difficulty level (beginner, intermediate, advanced)
- estimated_duration: Estimated learning time (e.g., "3 months", "6 months", "1 year")
- required_skills: List of skills that need to be mastered

Return ONLY JSON, no other content."""

ROADMAP_GENERATOR_PROMPT = """You are a learning roadmap planning expert. Based on the following goal analysis, generate structured learning phases.

Goal: {goal_name}
Goal Type: {goal_type}
Difficulty: {difficulty}
Estimated Time: {estimated_duration}
Required Skills: {required_skills}

Return a list of learning phases in JSON format. Each phase should contain:
- phase_name: Phase name
- phase_order: Phase order (starting from 1)
- phase_desc: Phase description (within 100 characters)
- estimated_days: Estimated days

Return ONLY a JSON array, no other content.
Example format: [{{"phase_name": "Java Basics", "phase_order": 1, "phase_desc": "...", "estimated_days": 30}}]"""

TASK_SPLITTER_PROMPT = """You are a learning task breakdown expert. Break down specific tasks for the following learning phase.

Learning Phase: {phase_name}
Phase Description: {phase_desc}
Estimated Days: {estimated_days}

Return a list of tasks in JSON format. Each task should contain:
- task_name: Task name
- task_desc: Task description (within 50 characters)
- priority: Priority (1=high, 2=medium, 3=low)
- estimated_hours: Estimated hours

Return ONLY a JSON array, no other content.
Example format: [{{"task_name": "Setup Environment", "task_desc": "Install JDK and IDE", "priority": 1, "estimated_hours": 2}}]"""

MASTERY_EVALUATOR_PROMPT = """You are a learning assessment expert. Evaluate the user's mastery level based on their learning data.

Goal: {goal_name}
Completion Rate: {completion_rate}%
Learning Hours: {learning_hours}
Test Score: {test_score}
Phases with Tasks: {phases_summary}

Return a JSON object with these fields:
- mastery_score: Overall mastery score (0-100)
- weaknesses: List of weak areas that need improvement
- suggestions: List of specific learning suggestions
- should_adjust: Boolean, whether the learning plan should be adjusted

Return ONLY JSON, no other content.
Example format: {{"mastery_score": 72, "weaknesses": ["Redis", "Microservices"], "suggestions": ["Review Redis basics", "Build a small Spring Boot project"], "should_adjust": true}}"""

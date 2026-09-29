# Hackathon Demo Script — Code Review Agent

## Step-by-Step Demo Walkthrough

1. **Public Home Page** (`http://localhost:8080/`):
   - Highlight public landing header (Home, Features, About, Login, Register).
   - Explain autonomous AI code review + Hindsight Memory concept.

2. **Registration & OTP Flow**:
   - Register a new developer account.
   - Show 6-digit OTP verification overlay with 30-second resend countdown timer.
   - Explain deferred database creation: User is created in PostgreSQL ONLY after OTP succeeds.

3. **Workspace Selection**:
   - Log in to see "Choose Your Workspace" prompt.
   - Choose between Individual Workspace or Team Workspace.

4. **Live Groq AI Code Review**:
   - Navigate to `/review.html`.
   - Paste Java/Python snippet.
   - Click "Run AI Code Review" to generate real-time feedback across 8 categories with Hindsight memory integration.

5. **Team Workspace Collaboration**:
   - Show Team ID (`TEAM-4KD9P1`) and 6-character Join Code (`CRA472`).
   - Demonstrate joining a team and member limit enforcement.

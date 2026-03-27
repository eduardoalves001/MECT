# MDRS_Projects

## Constituição do grupo

| NMec | Nome | Mail|
|:---:|:---|:---:|
| 104179 | EDUARDO ALVES | eduardoalves@ua.pt |
| 98392 | MARIANA SILVA | marianabarbara@ua.pt |

# PROJECT 2 INFO

# Task Files Summary

## Files Used by Each Task

| File | Task 1 | Task 2 | Task 3 |
|------|:------:|:------:|:------:|
| `InputDataProject2.mat` | ✓ | ✓ | ✓ |
| `kShortestPath.m` | ✓ | ✓ | ✓ |
| `greedyRandomInitialSolution.m` | ✓ | ✓ | ✓ |
| `computeLinkLoads.m` | ✓ | ✓ | ✓ |
| `computeNetworkEnergy.m` | ✓ | ✓ | ✓ |
| `hillClimbing.m` | ✓ | - | - |
| `evaluateWorstLinkLoad.m` | ✓ | - | - |
| `hillClimbingEnergyTask2.m` | - | ✓ | - |
| `evaluateEnergyTask2.m` | - | ✓ | - |
| `evaluateEnergyTask3.m` | - | - | ✓ |

---

## Key Differences Between Tasks

### Task 1: Load Balancing Optimization
- **Objective**: Minimize worst link load
- **Capacity**: Fixed at 50 Gbps per link
- **Algorithm**: `hillClimbing.m` + `evaluateWorstLinkLoad.m`
- **Goal**: Balance traffic distribution across network
- **Sections**: 
  - 1.a: Shortest path routing
  - 1.b: Energy consumption with shortest paths
  - 1.c: Multi-start hill climbing for load balancing
  - 1.d: Energy-aware load balancing

### Task 2: Energy Minimization (Fixed Capacity)
- **Objective**: Minimize total network energy consumption
- **Capacity**: Fixed at 50 Gbps per link
- **Constraint**: Link load ≤ 100% (50 Gbps)
- **Algorithm**: `hillClimbingEnergyTask2.m` + `evaluateEnergyTask2.m`
- **Goal**: Concentrate traffic to maximize sleeping links
- **Sections**:
  - 2.a: Algorithm development and path determination
  - 2.b: Execute with k=6 paths (30 seconds)
  - 2.c: Execute with all possible paths (30 seconds)

### Task 3: Energy Minimization (Variable Capacity)
- **Objective**: Minimize total network energy with capacity upgrades
- **Capacity**: Dynamic - can upgrade links from 50 to 100 Gbps
- **Constraint**: Link load ≤ link capacity (50 or 100 Gbps)
- **Algorithm**: Custom hill climbing + `evaluateEnergyTask3.m`
- **Goal**: Optimize energy while allowing strategic capacity upgrades
- **Sections**:
  - 3.a: Algorithm development with upgrade decisions
  - 3.b: Execute with k=6 paths (60 seconds)
  - 3.c: Additional scenarios (service increase)

---

## Common Files (Shared Across All Tasks)

These utility functions are used by all three tasks:

- **`InputDataProject2.mat`**: Network topology (L matrix), unicast flows (Tu), anycast flows (Ta)
- **`kShortestPath.m`**: Yen's algorithm for finding k-shortest paths between nodes
- **`greedyRandomInitialSolution.m`**: Generates randomized initial routing solutions
- **`computeLinkLoads.m`**: Calculates traffic load on each physical link
- **`computeNetworkEnergy.m`**: Computes total energy (routers + links)

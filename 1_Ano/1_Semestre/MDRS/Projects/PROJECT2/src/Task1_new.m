clear; 
close all; 
clc;
load('InputDataProject2.mat');

nNodes = size(Nodes, 1);
lc     = 50;    % link capacity (Gbps)
nc     = 500;   % node capacity (Gbps)
anycastNodes = [5 12];

nFlows_uni = size(Tu, 1);
nFlows_any = size(Ta, 1);

% TASK 1.a - Link loads (shortest path unicast + anycast)
k_uni = 1; % shortest path para unicast nesta task

% --- Unicast shortest paths (k=1) ---
[sP_uni, nSP_uni] = computeKShortestPathsUnicast(Tu, L, k_uni);

% --- Anycast shortest path para o anycast node mais próximo (fixo em todas as tasks) ---
[sP_any, nSP_any, Ta_routing] = computeAnycastShortestToClosest(nNodes, anycastNodes, L, Ta);

% --- Combinar tráfego e paths (unicast + anycast) ---
T   = [Tu; Ta_routing];
sP  = cat(2, sP_uni, sP_any);
nSP = cat(2, nSP_uni, nSP_any);

sol = ones(1, size(T,1));

% Calcular loads e energia dos links
[Loads_a, linkEnergy_a] = calculateLinkLoadEnergy(nNodes, Links, T, sP, sol, L, lc);
worstLoad_a = max(max(Loads_a(:, 3:4)));

fprintf("Task 1.a\n");

for i = 1:size(Loads_a,1)
    a = Loads_a(i,1);
    b = Loads_a(i,2);
    ab = Loads_a(i,3);
    ba = Loads_a(i,4);
    fprintf("{%2d-%2d}\t\t%8.2f\t\t%8.2f\t\t%6.1f%%\n", a, b, ab, ba, 100*max(ab,ba)/lc);
end

fprintf("\nWorst Link Load: %.2f Gbps (%.1f%%)\n\n", worstLoad_a, 100*worstLoad_a/lc);


% TASK 1.b - Network energy + sleeping links (para solução 1.a)
nodeEnergy_a = calculateNodeEnergy(T, sP, nNodes, nc, sol);
totalEnergy_a = linkEnergy_a + nodeEnergy_a;

sleepingLinks_a = buildSleepingLinksString(Loads_a);

fprintf("Task 1.b\n");
fprintf("Network energy consumption: %.2f\n", totalEnergy_a);
fprintf("Links in sleeping mode:%s\n\n", sleepingLinks_a);

% TASK 1.c + 1.d - Optimização (min worst link load), k=6, 30s
k_uni = 6;          
timeLimit = 30;   

% --- Candidate paths para unicast (k=6) ---
[sP_uni, nSP_uni] = computeKShortestPathsUnicast(Tu, L, k_uni);

% --- Anycast continua sempre shortest path para o mais próximo ---
[sP_any, nSP_any, Ta_routing] = computeAnycastShortestToClosest(nNodes, anycastNodes, L, Ta);

T   = [Tu; Ta_routing];
sP  = cat(2, sP_uni, sP_any);
nSP = cat(2, nSP_uni, nSP_any);

tStart = tic;

bestLoad = inf;
bestTotalEnergy = inf;
bestTime = inf;
bestSol = [];
bestLoads = [];
bestLinkEnergy = inf;

nSols = 0;

while toc(tStart) < timeLimit
    % Greedy Randomized solução inicial
    [sol0, Loads0, maxLoad0, linkEnergy0] = greedyRandomizedStrategy(nNodes, Links, T, sP, nSP, L, lc);

    while maxLoad0 > lc
        [sol0, Loads0, maxLoad0, linkEnergy0] = greedyRandomizedStrategy(nNodes, Links, T, sP, nSP, L, lc);
    end

    % Hill Climbing
    [sol1, Loads1, maxLoad1, linkEnergy1] = HillClimbingStrategy(nNodes, Links, T, sP, nSP, sol0, Loads0, linkEnergy0, L, lc);

    % Avaliar energia total (links + nós)
    nodeEnergy1 = calculateNodeEnergy(T, sP, nNodes, nc, sol1);
    totalEnergy1 = linkEnergy1 + nodeEnergy1;

    % Guardar melhor por WORST LOAD
    if maxLoad1 < bestLoad
        bestLoad = maxLoad1;
        bestTotalEnergy = totalEnergy1;
        bestTime = toc(tStart);
        bestSol = sol1;
        bestLoads = Loads1;
        bestLinkEnergy = linkEnergy1;
    end

    nSols = nSols + 1;
end

sleepingLinks_best = buildSleepingLinksString(bestLoads);

fprintf("Task 1.d (k=6, 30s)\n");
fprintf("Worst Link Load (best): %.2f Gbps\n", bestLoad);
fprintf("Network energy (best): %.2f\n", bestTotalEnergy);
fprintf("Best solution found at: %.2f s\n", bestTime);
fprintf("No. solutions evaluated: %d\n", nSols);
if isempty(sleepingLinks_best)
    fprintf("Links in sleeping mode (best): none\n");
else
    fprintf("Links in sleeping mode (best):%s\n", sleepingLinks_best);
end

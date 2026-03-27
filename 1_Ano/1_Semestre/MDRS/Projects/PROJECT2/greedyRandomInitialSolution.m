function sol = greedyRandomInitialSolution(paths)
% Generates a greedy randomized initial solution
% One path randomly selected per flow

nFlows = length(paths);
sol = zeros(nFlows,1);

for f = 1:nFlows
    nPaths = length(paths{f});
    sol(f) = randi(nPaths);
end
end

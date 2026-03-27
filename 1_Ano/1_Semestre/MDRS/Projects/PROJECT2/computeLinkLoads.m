function linkLoads = computeLinkLoads(sol, paths, Tu, nNodes, anycastPaths, Ta)
% Computes link loads for Task 2 and Task 3 (symmetrical routing)
% For each flow, the same path carries both upstream and downstream traffic
% Includes anycast traffic on fixed shortest paths
%
% Inputs:
%   sol          - Solution vector (path index for each unicast flow)
%   paths        - Cell array of candidate paths for unicast flows
%   Tu           - Unicast traffic matrix [source, dest, upstream, downstream]
%   nNodes       - Number of nodes in network
%   anycastPaths - (Optional) Cell array of fixed paths for anycast flows
%   Ta           - (Optional) Anycast traffic matrix [source, upstream, downstream]

linkLoads = zeros(nNodes);

% Add unicast traffic
for f = 1:length(sol)
    path = paths{f}{sol(f)};
    up = Tu(f,3);    % upstream bandwidth
    down = Tu(f,4);  % downstream bandwidth

    % Path carries upstream traffic (s -> d)
    for i = 1:length(path)-1
        a = path(i);
        b = path(i+1);
        linkLoads(a,b) = linkLoads(a,b) + up;
    end

    % Path carries downstream traffic (d -> s) - reverse direction
    for i = length(path):-1:2
        a = path(i);
        b = path(i-1);
        linkLoads(a,b) = linkLoads(a,b) + down;
    end
end

% Add anycast traffic (fixed shortest paths)
if nargin >= 5 && ~isempty(anycastPaths) && nargin >= 6 && ~isempty(Ta)
    for f = 1:length(anycastPaths)
        path = anycastPaths{f};
        up = Ta(f,2);    % upstream bandwidth
        down = Ta(f,3);  % downstream bandwidth

        % Path carries upstream traffic (source -> anycast node)
        for i = 1:length(path)-1
            a = path(i);
            b = path(i+1);
            linkLoads(a,b) = linkLoads(a,b) + up;
        end

        % Path carries downstream traffic (anycast node -> source) - reverse direction
        for i = length(path):-1:2
            a = path(i);
            b = path(i-1);
            linkLoads(a,b) = linkLoads(a,b) + down;
        end
    end
end
end

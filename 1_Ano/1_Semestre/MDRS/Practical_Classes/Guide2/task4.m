% Consider an ideal link (i.e., with a ber = 0) from one router to another router with a capacity of
% C Mbps (1 Mbps = 106 bps) for IP communications. The link has a propagation delay of 10 us
% (1 us = 10-6 seconds). There is a very large queue at the output port of the link. The IP packet
% flow supported by the link is characterized by:
% (i) the packet arrivals are a Poisson process with rate λ pps (packets/second)
% (ii) the size of each IP packet is between 64 and 1518 bytes (the size includes the overhead of
% the Layer 2 protocol) with the probabilities: 19% for 64 bytes, 23% for 110 bytes, 17% for
% 1518 bytes and an equal probability for all other values (i.e., from 65 to 109 and from 111
% to 1517).
% Consider that λ = 1000 pps and C = 10 Mbps. Determine:

lambda = 1000;
C = 10;

% 4a) the average packet size (in Bytes) and the average packet transmission time of the IP flow;
% answer: 620.02 Bytes, 4.96*10-4 seconds
prob_elementos = (1 - 0.19 - 0.23 - 0.17) / ((109 - 65 + 1) + (1517-111+1));
avg_packet_size = 0.19*64 + 0.23*110 + 0.17*1518 + sum((65:109)*(prob_elementos)) + sum((111:1517)*(prob_elementos));
avg_time = avg_packet_size * 8 / C * 1e-6;

fprintf('\nanswer of 4a): %.2f Bytes, %.2e seconds', avg_packet_size, avg_time);

% 4b) the average throughput (in Mbps) of the IP flow; answer: 4.96 Mbps
avg_packet_size_bits = avg_packet_size * 8; 
throughput_bps = lambda * avg_packet_size_bits; 
throughput_mbps = throughput_bps / 1e6;
fprintf('\nanswer of 4b): %.2f Mbps\n', throughput_mbps);

% 4c) the capacity of the link, in packets/second; answer: 2016.06 pps
C_bps = C * 1e6; 
avg_packet_size_bits = avg_packet_size * 8;
capacity_pps = C_bps / avg_packet_size_bits;

fprintf('\nanswer of 4c): %.2f pps\n', capacity_pps);

% 4d) the average packet queuing delay and average packet system delay of the IP flow (the
% system delay is the queuing delay + transmission time + propagation delay) using the
% M/G/1 queuing model; answer: queuing – 4.60*10-4 seconds, system – 9.66*10-4 seconds

t_prop = 10e-6; % propagation delay (s)

p64 = 0.19; p110 = 0.23; p1518 = 0.17;
rem = 1 - (p64 + p110 + p1518);
n1 = 109 - 65 + 1;
n2 = 1517 - 111 + 1;
prob_each = rem / (n1 + n2);

sizes = [64, 110, 1518, (65:109), (111:1517)]; 
probs = [p64, p110, p1518, repmat(prob_each,1,n1), repmat(prob_each,1,n2)];

E_L  = sum(sizes .* probs);       
E_L2 = sum((sizes.^2) .* probs);  

C_bps = C * 1e6; 
E_S  = (E_L * 8) / C_bps;           
E_S2 = ((8 / C_bps)^2) * E_L2;     

rho = lambda * E_S;

Wq = (lambda * E_S2) / (2 * (1 - rho));
W  = Wq + E_S + t_prop;                   

fprintf('\nanswer of 4d): queuing - %.2e seconds, system - %.2e seconds\n', Wq, W);

% 4e) draw a plot with the same look as the plot below with the average packet system delay as
% a function of the packet arrival rate λ (from λ = 100 pps up to λ = 2000 pps); analyze the
% results and take conclusions.

lambda_vals = 100:100:2000;
W_vals = zeros(size(lambda_vals));

for i = 1:length(lambda_vals)
    lam = lambda_vals(i);
    rho = lam * E_S;
    if rho < 1
        Wq = (lam * E_S2) / (2 * (1 - rho));
        W_vals(i) = Wq + E_S + t_prop;
    else
        W_vals(i) = NaN; 
    end
end

figure(1);
plot(lambda_vals, W_vals*1e3, 'b-', 'LineWidth', 2);
xlabel('\lambda (packets/sec)');
ylabel('Average System Delay (ms)');
title('Average Packet System Delay vs Arrival Rate (\lambda)');
grid on;